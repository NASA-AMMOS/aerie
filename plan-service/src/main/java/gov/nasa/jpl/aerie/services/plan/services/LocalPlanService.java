package gov.nasa.jpl.aerie.services.plan.services;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.services.plan.models.AdaptationFacade;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.services.plan.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.services.plan.exceptions.ValidationException;
import gov.nasa.jpl.aerie.services.plan.models.ActivityInstance;
import gov.nasa.jpl.aerie.services.plan.models.NewPlan;
import gov.nasa.jpl.aerie.services.plan.models.Plan;
import gov.nasa.jpl.aerie.services.plan.remotes.PlanRepository;
import gov.nasa.jpl.aerie.services.plan.remotes.PlanRepository.PlanTransaction;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class LocalPlanService implements PlanService {
  private final PlanRepository planRepository;
  private final AdaptationService adaptationService;

  public LocalPlanService(
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
    withValidator(validator -> validator.validateNewPlan(plan));
    return this.planRepository.createPlan(plan);
  }

  @Override
  public void removePlan(final String id) throws NoSuchPlanException {
    this.planRepository.deletePlan(id);
  }

  @Override
  public void updatePlan(final String planId, final Plan patch) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    final String adaptationId = this.planRepository.getPlan(planId).adaptationId;
    withValidator(validator -> validator.validatePlanPatch(adaptationId, planId, patch));

    final PlanTransaction transaction = this.planRepository.updatePlan(planId);
    if (patch.name != null) transaction.setName(patch.name);
    if (patch.startTimestamp != null) transaction.setStartTimestamp(patch.startTimestamp);
    if (patch.endTimestamp != null) transaction.setEndTimestamp(patch.endTimestamp);
    transaction.commit();

    if (patch.activityInstances != null) {
      for (final var entry : patch.activityInstances.entrySet()) {
        if (entry.getValue() == null) {
          this.removeActivityInstanceById(planId, entry.getKey());
        } else {
          this.replaceActivityInstance(planId, entry.getKey(), entry.getValue());
        }
      }
    }
  }

  @Override
  public void replacePlan(final String id, final NewPlan plan) throws ValidationException, NoSuchPlanException {
    withValidator(validator -> validator.validateNewPlan(plan));
    this.planRepository.replacePlan(id, plan);
  }

  @Override
  public ActivityInstance getActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    return this.planRepository.getActivityInPlanById(planId, activityInstanceId);
  }

  @Override
  public List<String> addActivityInstancesToPlan(final String planId, final List<ActivityInstance> activityInstances) throws ValidationException, NoSuchPlanException {
    final String adaptationId = this.planRepository.getPlan(planId).adaptationId;
    withValidator(validator -> validator.validateActivityList(adaptationId, activityInstances));

    final List<String> activityInstanceIds = new ArrayList<>(activityInstances.size());
    for (final ActivityInstance activityInstance : activityInstances) {
      final String activityInstanceId = this.planRepository.createActivity(planId, activityInstance);
      activityInstanceIds.add(activityInstanceId);
    }

    return activityInstanceIds;
  }

  @Override
  public void removeActivityInstanceById(final String planId, final String activityInstanceId) throws NoSuchPlanException, NoSuchActivityInstanceException {
    this.planRepository.deleteActivity(planId, activityInstanceId);
  }

  @Override
  public void updateActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance patch) throws NoSuchPlanException, NoSuchActivityInstanceException, ValidationException {
    final Plan plan = this.planRepository.getPlan(planId);

    final ActivityInstance activityInstance = Optional
        .ofNullable(plan.activityInstances.getOrDefault(activityInstanceId, null))
        .orElseThrow(() -> new NoSuchActivityInstanceException(planId, activityInstanceId));

    if (patch.type != null) activityInstance.type = patch.type;
    if (patch.startTimestamp != null) activityInstance.startTimestamp = patch.startTimestamp;
    if (patch.parameters != null) activityInstance.parameters = patch.parameters;

    withValidator(validator -> validator.validateActivity(plan.adaptationId, activityInstance));

    this.planRepository.replaceActivity(planId, activityInstanceId, activityInstance);
  }

  @Override
  public void replaceActivityInstance(final String planId, final String activityInstanceId, final ActivityInstance activityInstance) throws ValidationException, NoSuchPlanException, NoSuchActivityInstanceException {
    final String adaptationId = this.planRepository.getPlan(planId).adaptationId;
    withValidator(validator -> validator.validateActivity(adaptationId, activityInstance));

    this.planRepository.replaceActivity(planId, activityInstanceId, activityInstance);
  }

  @Override
  public SimulationResults getSimulationResultsForPlan(final String planId)
  throws NoSuchPlanException
  {
    final var plan = this.planRepository.getPlan(planId);

    try {
      return this.adaptationService.runSimulation(new CreateSimulationMessage(
          plan.adaptationId,
          plan.startTimestamp.toInstant(),
          Duration.of(
              plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
              Duration.MICROSECONDS),
          serializeScheduledActivities(plan.startTimestamp.toInstant(), plan.activityInstances)));
    } catch (final AdaptationService.NoSuchAdaptationException ex) {
      throw new RuntimeException("Assumption falsified -- adaptation for existing plan does not exist");
    } catch (final SimulationDriver.TaskSpecInstantiationException | AdaptationFacade.NoSuchActivityTypeException ex) {
      throw new RuntimeException("Assumption falsified -- activity could not be instantiated");
    }
  }

  private Map<String, Pair<Duration, SerializedActivity>>
  serializeScheduledActivities(final Instant startTime, final Map<String, ActivityInstance> activityInstances) {
    final var scheduledActivities = new HashMap<String, Pair<Duration, SerializedActivity>>();

    for (final var entry : activityInstances.entrySet()) {
      final var id = entry.getKey();
      final var activity = entry.getValue();

      scheduledActivities.put(id, Pair.of(
          Duration.of(startTime.until(activity.startTimestamp.toInstant(), ChronoUnit.MICROS), Duration.MICROSECONDS),
          new SerializedActivity(activity.type, activity.parameters)));
    }

    return scheduledActivities;
  }

  private <T extends Throwable> void withValidator(final ValidationScope<T> block) throws ValidationException, T {
    final var validator = new PlanValidator(this.planRepository, this.adaptationService);

    block.accept(validator);
    final var messages = validator.getMessages();

    if (messages.size() > 0) throw new ValidationException(messages);
  }

  @FunctionalInterface
  private interface ValidationScope<T extends Throwable> {
    void accept(PlanValidator validator) throws T;
  }
}
