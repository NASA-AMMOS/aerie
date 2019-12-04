package gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public final class PlanValidator {
  private final PlanRepository planRepository;
  private final AdaptationService adaptationService;

  private final BreadcrumbCursor breadcrumbCursor = new BreadcrumbCursor();
  private final List<Pair<List<Breadcrumb>, String>> messages = new ArrayList<>();

  public PlanValidator(final PlanRepository planRepository, final AdaptationService adaptationService) {
    this.planRepository = planRepository;
    this.adaptationService = adaptationService;
  }

  public List<Pair<List<Breadcrumb>, String>> getMessages() {
    return List.copyOf(this.messages);
  }

  private void validateParameter(final SerializedParameter parameter, final ParameterSchema parameterSchema) {
    parameterSchema.match(new ParameterSchema.Visitor<>() {
      @Override
      public Object onReal() {
        if (parameter.asReal().isEmpty()) addError("Expected real number");
        return null;
      }

      @Override
      public Object onInt() {
        if (parameter.asInt().isEmpty()) addError("Expected integral number");
        return null;
      }

      @Override
      public Object onBoolean() {
        if (parameter.asBoolean().isEmpty()) addError("Expected boolean");
        return null;
      }

      @Override
      public Object onString() {
        if (parameter.asString().isEmpty()) addError("Expected boolean");
        return null;
      }

      @Override
      public Object onList(final ParameterSchema itemSchema) {
        if (parameter.isNull()) {
          // pass
        } else if (parameter.asList().isPresent()) {
          int index = 0;
          for (final var item : parameter.asList().get()) {
            with(index++, () -> validateParameter(item, itemSchema));
          }
        } else {
          addError("Expected list");
        }

        return null;
      }

      @Override
      public Object onMap(final Map<String, ParameterSchema> fieldSchemas) {
        if (parameter.isNull()) {
          // pass
        } else if (parameter.asMap().isPresent()) {
          validateParameterMap(parameter.asMap().get(), fieldSchemas);
        } else {
          addError("Expected map");
        }

        return null;
      }
    });
  }

  private void validateParameterMap(
      final Map<String, SerializedParameter> activityParameters,
      final Map<String, ParameterSchema> fieldSchemas
  ) {
    for (final var entry : activityParameters.entrySet()) {
      final ParameterSchema fieldSchema = fieldSchemas.getOrDefault(entry.getKey(), null);

      with(entry.getKey(), () -> {
        if (fieldSchema == null) {
          addError("unknown parameter");
        } else {
          validateParameter(entry.getValue(), fieldSchema);
        }
      });
    }
  }

  public void validateActivity(
      final ActivityInstance activityInstance,
      final Map<String, Map<String, ParameterSchema>> activityTypes
  ) {
    if (activityInstance.startTimestamp == null) with("startTimestamp", () -> addError("must be non-null"));

    if (activityInstance.type == null) {
      with("type", () -> addError("must be non-null"));
    } else if (!activityTypes.containsKey(activityInstance.type)) {
      with("type", () -> addError("unknown activity type"));
    } else if (activityInstance.parameters != null) {
      with("parameters", () -> validateParameterMap(activityInstance.parameters, activityTypes.get(activityInstance.type)));
    }
  }

  public void validateActivityList(
      final Collection<ActivityInstance> activityInstances,
      final Map<String, Map<String, ParameterSchema>> activityTypes
  ) {
    int index = 0;
    for (final ActivityInstance activityInstance : activityInstances) {
      with(index++, () -> validateActivity(activityInstance, activityTypes));
    }
  }

  public void validateActivityMap(
      final Map<String, ActivityInstance> activityInstances,
      final Map<String, Map<String, ParameterSchema>> activityTypes
  ) {
    for (final var entry : activityInstances.entrySet()) {
      final String activityId = entry.getKey();
      final ActivityInstance activityInstance = entry.getValue();

      with(activityId, () -> validateActivity(activityInstance, activityTypes));
    }
  }

  public void validateNewPlan(final NewPlan plan) {
    if (plan.name == null) with("name", () -> addError("must be non-null"));
    if (plan.startTimestamp == null) with("startTimestamp", () -> addError("must be non-null"));
    if (plan.endTimestamp == null) with("endTimestamp", () -> addError("must be non-null"));

    Map<String, Map<String, ParameterSchema>> activityTypes = null;
    if (plan.adaptationId == null) {
      with("adaptationId", () -> addError("must be non-null"));
    } else {
      try {
        activityTypes = this.adaptationService.getActivityTypes(plan.adaptationId);
      } catch (final NoSuchAdaptationException e) {
        with("adaptationId", () -> addError("no adaptation with given adaptationId"));
      }
    }

    if (plan.activityInstances != null) {
      final var finalActivityTypes = activityTypes;
      with("activityInstances", () -> validateActivityList(plan.activityInstances, finalActivityTypes));
    }
  }

  public void validatePlanPatch(final String planId, final Plan patch) throws NoSuchPlanException {
    Map<String, Map<String, ParameterSchema>> activityTypes = null;
    if (patch.adaptationId != null) {
      try {
        activityTypes = this.adaptationService.getActivityTypes(patch.adaptationId);
      } catch (final NoSuchAdaptationException e) {
        with("adaptationId", () -> addError("no adaptation with given id"));
      }
    } else if (patch.activityInstances != null) {
      final String adaptationId = this.planRepository.getPlan(planId).adaptationId;
      try {
        activityTypes = this.adaptationService.getActivityTypes(adaptationId);
      } catch (final NoSuchAdaptationException ex) {
        throw new RuntimeException("Unexpectedly missing adaptation `" + adaptationId + "` referenced by plan `" + planId + "`", ex);
      }
    }

    if (patch.activityInstances != null) {
      final Set<String> validActivityIds = this.planRepository
          .getAllActivitiesInPlan(planId)
          .map(Pair::getKey)
          .collect(Collectors.toSet());

      final var finalActivityTypes = activityTypes;
      with("activityInstances", () -> {
        for (final String activityId : patch.activityInstances.keySet()) {
          if (!validActivityIds.contains(activityId)) {
            with(activityId, () -> addError("no activity with id in plan"));
          }
        }

        validateActivityMap(patch.activityInstances, finalActivityTypes);
      });
    }
  }

  private void addError(final String message) {
    this.messages.add(Pair.of(unmodifiableList(this.breadcrumbCursor.getPath()), message));
  }

  private void with(final int index, final Runnable block) {
    this.breadcrumbCursor.descend(index);
    try {
      block.run();
    } finally {
      this.breadcrumbCursor.ascend();
    }
  }

  private void with(final String index, final Runnable block) {
    this.breadcrumbCursor.descend(index);
    try {
      block.run();
    } finally {
      this.breadcrumbCursor.ascend();
    }
  }
}
