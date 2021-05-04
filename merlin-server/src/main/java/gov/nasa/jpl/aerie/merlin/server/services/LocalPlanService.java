package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.json.ConstraintParsers;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.driver.SimulationDriver;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.Duration;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchActivityInstanceException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.ValidationException;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityInstance;
import gov.nasa.jpl.aerie.merlin.server.models.AdaptationFacade;
import gov.nasa.jpl.aerie.merlin.server.models.NewPlan;
import gov.nasa.jpl.aerie.merlin.server.models.Plan;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository.PlanTransaction;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.Json;
import java.io.StringReader;
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
  public Pair<SimulationResults, Map<String, List<Violation>>> getSimulationResultsForPlan(final String planId)
  throws NoSuchPlanException
  {
    try {
      final var plan = this.planRepository.getPlan(planId);

      final var planDuration = Duration.of(
          plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
          Duration.MICROSECONDS);

      final var results = this.adaptationService.runSimulation(new CreateSimulationMessage(
          plan.adaptationId,
          plan.startTimestamp.toInstant(),
          planDuration,
          serializeScheduledActivities(plan.startTimestamp.toInstant(), plan.activityInstances)));

      final var activities = new ArrayList<gov.nasa.jpl.aerie.constraints.model.ActivityInstance>();
      for (final var entry : results.simulatedActivities.entrySet()) {
        final var id = entry.getKey();
        final var activity = entry.getValue();

        final var activityOffset = Duration.of(
          plan.startTimestamp.toInstant().until(activity.start, ChronoUnit.MICROS),
          Duration.MICROSECONDS);

        activities.add(new gov.nasa.jpl.aerie.constraints.model.ActivityInstance(
            id,
            activity.type,
            activity.parameters,
            Window.between(activityOffset, activityOffset.plus(activity.duration))));
      }

      final var discreteProfiles = new HashMap<String, DiscreteProfile>(results.discreteProfiles.size());
      for (final var entry : results.discreteProfiles.entrySet()) {
        final var pieces = new ArrayList<DiscreteProfilePiece>(entry.getValue().getRight().size());

        var elapsed = Duration.ZERO;
        for (final var piece : entry.getValue().getRight()) {
          final var extent = piece.getLeft();
          final var value = piece.getRight();

          pieces.add(new DiscreteProfilePiece(
              Window.between(elapsed, elapsed.plus(extent)),
              value));

          elapsed = elapsed.plus(extent);
        }

        discreteProfiles.put(entry.getKey(), new DiscreteProfile(pieces));
      }

      final var realProfiles = new HashMap<String, LinearProfile>();
      for (final var entry : results.realProfiles.entrySet()) {
        final var pieces = new ArrayList<LinearProfilePiece>(entry.getValue().size());

        var elapsed = Duration.ZERO;
        for (final var piece : entry.getValue()) {
          final var extent = piece.getLeft();
          final var value = piece.getRight();

          pieces.add(new LinearProfilePiece(
              Window.between(elapsed, elapsed.plus(extent)),
              value.initial,
              value.rate));

          elapsed = elapsed.plus(extent);
        }

        realProfiles.put(entry.getKey(), new LinearProfile(pieces));
      }

      final var preparedResults = new gov.nasa.jpl.aerie.constraints.model.SimulationResults(
          Window.between(Duration.ZERO, planDuration),
          activities,
          realProfiles,
          discreteProfiles);

      final var constraintJsons = this.adaptationService.getConstraints(plan.adaptationId);
      final var violations = new HashMap<String, List<Violation>>();
      for (final var entry : constraintJsons.entrySet()) {
        final var subject = Json.createReader(new StringReader(entry.getValue())).readValue();
        final var constraint = ConstraintParsers.constraintP.parse(subject);

        if (constraint.isFailure()) {
          throw new Error(entry.getValue());
        }

        final var violationEvents = constraint.getSuccessOrThrow().evaluate(preparedResults);

        if (violationEvents.isEmpty()) continue;

        /* TODO: constraint.evaluate returns an List<Violations> with a single empty unpopulated Violation
            which prevents the above condition being sufficient in all cases. A ticket AERIE-1230 has been
            created to account for refactoring and removing the need for this condition. */
        if (violationEvents.size() == 1 && violationEvents.get(0).violationWindows.isEmpty()) continue;

        violations.put(entry.getKey(), violationEvents);
      }

      return Pair.of(results, violations);
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
