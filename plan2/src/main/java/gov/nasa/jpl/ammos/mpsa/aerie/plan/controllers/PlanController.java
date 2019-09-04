package gov.nasa.jpl.ammos.mpsa.aerie.plan.controllers;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.NewPlan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.models.Plan;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.AdaptationService.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.remotes.PlanRepository.PlanTransaction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    return this.planRepository
        .getAllPlans()
        .map(transaction -> Pair.of(transaction.getId(), transaction.get()));
  }

  @Override
  public Plan getPlanById(final String id) throws NoSuchPlanException {
    return this.planRepository
        .getPlan(id)
        .orElseThrow(() -> new NoSuchPlanException(id))
        .get();
  }

  @Override
  public String addPlan(final NewPlan plan) throws ValidationException {
    final PlanTransaction transaction = this.planRepository.newPlan();

    applyNewPlanToTransaction(transaction, plan);
    validatePlan(transaction.get());

    return transaction.save();
  }

  @Override
  public void removePlan(final String id) throws NoSuchPlanException {
    this.planRepository
        .getPlan(id)
        .orElseThrow(() -> new NoSuchPlanException(id))
        .delete();
  }

  @Override
  public void updatePlan(final String id, final Plan patch) throws ValidationException, NoSuchPlanException {
    final PlanTransaction transaction = this.planRepository
        .getPlan(id)
        .orElseThrow(() -> new NoSuchPlanException(id));

    patchPlanToTransaction(transaction, patch);
    validatePlan(transaction.get());

    transaction.save();
  }

  @Override
  public void replacePlan(final String id, final NewPlan plan) throws ValidationException, NoSuchPlanException {
    final PlanTransaction transaction = this.planRepository
        .getPlan(id)
        .orElseThrow(() -> new NoSuchPlanException(id));

    applyNewPlanToTransaction(transaction, plan);
    validatePlan(transaction.get());

    transaction.save();
  }

  private void validatePlan(final Plan plan) throws ValidationException {
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
    if (plan.activityInstances == null) {
      validationErrors.add("activityInstances must be non-null");
    }

    if (plan.adaptationId == null) {
      validationErrors.add("adaptationId must be non-null");
    } else {
      final Optional<Adaptation> adaptation = adaptationService.getAdaptationById(plan.adaptationId);
      if (adaptation.isEmpty()) {
        validationErrors.add("no adaptation with given adaptationId");
      } else {
        final Map<String, ActivityType> activityTypes = adaptation.get().getActivityTypes();

        // TODO: Validate the plan's activity instances against the adaptation's activity types.
      }
    }

    if (validationErrors.size() > 0) {
      throw new ValidationException("invalid plan", validationErrors);
    }
  }

  private void applyNewPlanToTransaction(final PlanTransaction transaction, final NewPlan newPlan) {
    transaction
        .setName(newPlan.name)
        .setAdaptationId(newPlan.adaptationId)
        .setStartTimestamp(newPlan.startTimestamp)
        .setEndTimestamp(newPlan.endTimestamp);

    if (newPlan.activityInstances != null) {
      for (final ActivityInstance activity : newPlan.activityInstances) {
        transaction.newActivity()
            .setType(activity.type)
            .setStartTimestamp(activity.startTimestamp)
            .setParameters(activity.parameters)
            .save();
      }
    }
  }

  private void patchPlanToTransaction(final PlanTransaction transaction, final Plan patch) throws ValidationException {
    Optional.ofNullable(patch.name).ifPresent(transaction::setName);
    Optional.ofNullable(patch.adaptationId).ifPresent(transaction::setAdaptationId);
    Optional.ofNullable(patch.startTimestamp).ifPresent(transaction::setStartTimestamp);
    Optional.ofNullable(patch.endTimestamp).ifPresent(transaction::setEndTimestamp);

    final Map<String, ActivityInstance> activities = Objects.requireNonNullElseGet(patch.activityInstances, Map::of);
    for (final var entry : activities.entrySet()) {
      final String activityId = entry.getKey();
      final ActivityInstance activityPatch = entry.getValue();

      final PlanRepository.ActivityTransaction activityTransaction = transaction
          .getActivity(activityId)
          .orElseThrow(() -> new ValidationException("invalid patch", List.of("no activity with id" + activityId)));

      Optional.ofNullable(activityPatch.type).ifPresent(activityTransaction::setType);
      Optional.ofNullable(activityPatch.startTimestamp).ifPresent(activityTransaction::setStartTimestamp);
      Optional.ofNullable(activityPatch.parameters).ifPresent(activityTransaction::setParameters);

      activityTransaction.save();
    }
  }
}
