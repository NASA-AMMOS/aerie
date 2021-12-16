package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableList;

public final class PlanValidator {
  private final PlanRepository planRepository;
  private final MissionModelService missionModelService;

  private final BreadcrumbCursor breadcrumbCursor = new BreadcrumbCursor();
  private final List<Pair<List<Breadcrumb>, String>> messages = new ArrayList<>();

  public PlanValidator(final PlanRepository planRepository, final MissionModelService missionModelService) {
    this.planRepository = planRepository;
    this.missionModelService = missionModelService;
  }

  public void validateActivity(final String missionModelId, final ActivityInstance activityInstance) {
    final List<String> validationFailures;
    try {
      validationFailures = this.missionModelService.validateActivityParameters(
          missionModelId,
          new SerializedActivity(activityInstance.type, activityInstance.arguments));
    } catch (final MissionModelService.NoSuchMissionModelException ex) {
      throw new Error("Unexpectedly nonexistent mission model, when this should have been validated earlier.", ex);
    }

    for (final var failure : validationFailures) addError(failure);

    if (activityInstance.startTimestamp == null) with("startTimestamp", () -> addError("must be non-null"));
    if (activityInstance.type == null) with("type", () -> addError("must be non-null"));
  }

  public void validateActivityList(final String missionModelId, final Collection<ActivityInstance> activityInstances) {
    int index = 0;
    for (final ActivityInstance activityInstance : activityInstances) {
      with(index++, () -> validateActivity(missionModelId, activityInstance));
    }
  }

  public void validateNewPlan(final NewPlan plan) {
    if (plan.name == null) with("name", () -> addError("must be non-null"));
    if (plan.startTimestamp == null) with("startTimestamp", () -> addError("must be non-null"));
    if (plan.endTimestamp == null) with("endTimestamp", () -> addError("must be non-null"));
    if (plan.missionModelId == null) {
      with("missionModelId", () -> addError("must be non-null"));
    } else if (!missionModelExists(plan.missionModelId)) {
      with("missionModelId", () -> addError("is not a defined mission model"));
    } else if (plan.activityInstances != null) {
      with("activityInstances", () -> validateActivityList(plan.missionModelId, plan.activityInstances));
    }
  }

  private boolean missionModelExists(final String missionModelId) {
    try {
      this.missionModelService.getMissionModelById(missionModelId);
      return true;
    } catch (final MissionModelService.NoSuchMissionModelException ex) {
      return false;
    }
  }

  public void validatePlanPatch(final String missionModelId, final String planId, final Plan patch) throws NoSuchPlanException {
    if (patch.missionModelId != null) with("missionModelId", () -> addError("cannot be changed after creation"));

    if (patch.activityInstances != null) {
      final Set<String> validActivityIds = this.planRepository
          .getAllActivitiesInPlan(planId)
          .keySet();

      with("activityInstances", () -> {
        for (final var entry : patch.activityInstances.entrySet()) {
          final var activityId = entry.getKey();
          final var activityInstance = entry.getValue();
          if (!validActivityIds.contains(activityId)) {
            with(activityId, () -> addError("no activity with id in plan"));
          }

          if (activityInstance != null)
            with(activityId, () -> validateActivity(missionModelId, activityInstance));
        }
      });
    }
  }

  public List<Pair<List<Breadcrumb>, String>> getMessages() {
    return List.copyOf(this.messages);
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
