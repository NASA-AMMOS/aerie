package gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.UnexpectedMissingAdaptationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions.ValidationException;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository.PlanTransaction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class PlanController implements IPlanController {
  private final PlanRepository planRepository;
  private final AdaptationService adaptationService;

  public PlanController(
      final PlanRepository planRepository,
      final AdaptationService adaptationService
  ) {
    this.planRepository = planRepository;
    this.adaptationService = adaptationService;
  }

  @Override
  public Stream<Pair<String, Plan>> getPlans() {
    return this.planRepository.getAllPlans();
  }

  @Override
  public Plan getPlanById(final String id) throws NoSuchPlanException {
    return this.planRepository.getPlan(id);
  }

  @Override
  public String addPlan(final NewPlan plan) throws ValidationException {
    validateNewPlan(plan);
    return this.planRepository.createPlan(plan);
  }

  @Override
  public void removePlan(final String id) throws NoSuchPlanException {
    this.planRepository.deletePlan(id);
  }

  @Override
  public void updatePlan(final String id, final Plan patch) throws ValidationException, NoSuchPlanException {
    validatePlanPatch(patch);

    final PlanTransaction transaction = this.planRepository.updatePlan(id);
    if (patch.name != null) transaction.setName(patch.name);
    if (patch.startTimestamp != null) transaction.setStartTimestamp(patch.startTimestamp);
    if (patch.endTimestamp != null) transaction.setEndTimestamp(patch.endTimestamp);
    if (patch.adaptationId != null) transaction.setAdaptationId(patch.adaptationId);

    transaction.commit();
  }

  @Override
  public void replacePlan(final String id, final NewPlan plan) throws ValidationException, NoSuchPlanException {
    validateNewPlan(plan);
    this.planRepository.replacePlan(id, plan);
  }

  @Override
  public ActivityInstance getActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    return this.planRepository.getActivityInPlanById(planId, activityInstanceId);
  }

  @Override
  public List<String> addActivityInstancesToPlan(final String planId, final List<ActivityInstance> activityInstances) throws ValidationException, NoSuchPlanException {
    {
      final String adaptationId = this.planRepository.getPlan(planId).adaptationId;

      final Map<String, ActivityType> activityTypes;
      try {
        activityTypes = this.adaptationService.getActivityTypes(adaptationId);
      } catch (final NoSuchAdaptationException ex) {
        throw new UnexpectedMissingAdaptationException(adaptationId, ex);
      }

      validateActivities(activityInstances, Optional.of(activityTypes));
    }

    final List<String> activityInstanceIds = new ArrayList<>(activityInstances.size());
    for (final ActivityInstance activityInstance : activityInstances) {
      final String activityInstanceId = this.planRepository.createActivity(planId, activityInstance);
      activityInstanceIds.add(activityInstanceId);
    }

    return activityInstanceIds;
  }

  private void validateActivities(final Collection<ActivityInstance> activityInstances, final Optional<Map<String, ActivityType>> activityTypes) throws ValidationException {
    // TODO: Validate each activity instance against the associated adaptation's activity types.
    // TODO: Validate that each activity is structurally valid.
  }

  private void validateNewPlan(final NewPlan plan) throws ValidationException {
    final List<String> validationErrors = new ArrayList<>();

    if (plan.name == null) {
      validationErrors.add("name must be non-null");
    }
    if (plan.startTimestamp == null) {
      validationErrors.add("startTimestamp must be non-null");
    }
    if (plan.endTimestamp == null) {
      validationErrors.add("endTimestamp must be non-null");
    }

    if (plan.adaptationId == null) {
      validationErrors.add("adaptationId must be non-null");
    } else {
      try {
        final Map<String, ActivityType> activityTypes = this.adaptationService.getActivityTypes(plan.adaptationId);

        // TODO: Validate the plan's activity instances against the adaptation's activity types.
      } catch (final NoSuchAdaptationException e) {
        validationErrors.add("no adaptation with given adaptationId");
      }
    }

    if (validationErrors.size() > 0) {
      throw new ValidationException("invalid plan", validationErrors);
    }
  }

  private void validatePlanPatch(final Plan patch) throws ValidationException {
    final List<String> validationErrors = new ArrayList<>();
    final Optional<Map<String, ActivityType>> activityTypes;

    if (patch.adaptationId == null) {
      activityTypes = Optional.empty();
    } else {
      try {
        activityTypes = Optional.of(this.adaptationService.getActivityTypes(patch.adaptationId));
      } catch (final NoSuchAdaptationException e) {
        validationErrors.add("no adaptation with given adaptationId");
      }
    }

    if (patch.activityInstances != null) {
      // TODO: Validate the plan's activity instances against the adaptation's activity types.
    }

    if (validationErrors.size() > 0) {
      throw new ValidationException("invalid plan", validationErrors);
    }
  }
}
