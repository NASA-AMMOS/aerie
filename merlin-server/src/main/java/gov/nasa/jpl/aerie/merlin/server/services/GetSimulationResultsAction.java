package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.merlin.driver.SimulationFailure;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationResultsHandle;
import org.apache.commons.lang3.tuple.Pair;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class GetSimulationResultsAction {
  public sealed interface Response {
    record Pending(long simulationDatasetId) implements Response {}
    record Incomplete(long simulationDatasetId) implements Response {}
    record Failed(long simulationDatasetId, SimulationFailure reason) implements Response {}
    record Complete(long simulationDatasetId) implements Response {}
  }

  private final PlanService planService;
  private final MissionModelService missionModelService;
  private final SimulationService simulationService;
  private final ConstraintsDSLCompilationService constraintsDSLCompilationService;

  public GetSimulationResultsAction(
      final PlanService planService,
      final MissionModelService missionModelService,
      final SimulationService simulationService,
      final ConstraintsDSLCompilationService constraintsDSLCompilationService
  ) {
    this.planService = Objects.requireNonNull(planService);
    this.missionModelService = Objects.requireNonNull(missionModelService);
    this.simulationService = Objects.requireNonNull(simulationService);
    this.constraintsDSLCompilationService = Objects.requireNonNull(constraintsDSLCompilationService);
  }

  public Response run(final PlanId planId) throws NoSuchPlanException, MissionModelService.NoSuchMissionModelException {
    final var revisionData = this.planService.getPlanRevisionData(planId);

    final var response = this.simulationService.getSimulationResults(planId, revisionData);

    if (response instanceof ResultsProtocol.State.Pending r) {
      return new Response.Pending(r.simulationDatasetId());
    } else if (response instanceof ResultsProtocol.State.Incomplete r) {
      return new Response.Incomplete(r.simulationDatasetId());
    } else if (response instanceof ResultsProtocol.State.Failed r) {
      return new Response.Failed(r.simulationDatasetId(), r.reason());
    } else if (response instanceof ResultsProtocol.State.Success r) {
      return new Response.Complete(r.simulationDatasetId());
    } else {
      throw new UnexpectedSubtypeError(ResultsProtocol.State.class, response);
    }
  }

  public Map<String, List<Pair<Duration, SerializedValue>>> getResourceSamples(final PlanId planId)
  throws NoSuchPlanException
  {
    final var revisionData = this.planService.getPlanRevisionData(planId);
    final var simulationResults$ = this.simulationService.get(planId, revisionData);
    if (simulationResults$.isEmpty()) return Collections.emptyMap();
    final var simulationResults = simulationResults$.get().getSimulationResults();

    final var samples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();

    simulationResults.realProfiles.forEach((name, p) -> {
      var elapsed = Duration.ZERO;
      var profile = p.getRight();

      final var timeline = new ArrayList<Pair<Duration, SerializedValue>>();
      for (final var piece : profile) {
        final var extent = piece.extent();
        final var dynamics = piece.dynamics();

        timeline.add(Pair.of(elapsed, SerializedValue.of(
            dynamics.initial)));
        elapsed = elapsed.plus(extent);
        timeline.add(Pair.of(elapsed, SerializedValue.of(
            dynamics.initial + dynamics.rate * extent.ratioOver(Duration.SECONDS))));
      }

      samples.put(name, timeline);
    });
    simulationResults.discreteProfiles.forEach((name, p) -> {
      var elapsed = Duration.ZERO;
      var profile = p.getRight();

      final var timeline = new ArrayList<Pair<Duration, SerializedValue>>();
      for (final var piece : profile) {
        final var extent = piece.extent();
        final var value = piece.dynamics();

        timeline.add(Pair.of(elapsed, value));
        elapsed = elapsed.plus(extent);
        timeline.add(Pair.of(elapsed, value));
      }

      samples.put(name, timeline);
    });

    return samples;
  }

  public List<Violation> getViolations(final PlanId planId)
  throws NoSuchPlanException, MissionModelService.NoSuchMissionModelException
  {
    final var plan = this.planService.getPlanForValidation(planId);
    final var revisionData = this.planService.getPlanRevisionData(planId);

    final var constraintCode = new HashMap<Long, Constraint>();

    try {
      constraintCode.putAll(this.missionModelService.getConstraints(plan.missionModelId));
      constraintCode.putAll(this.planService.getConstraintsForPlan(planId));
    } catch (final MissionModelService.NoSuchMissionModelException ex) {
      throw new RuntimeException("Assumption falsified -- mission model for existing plan does not exist");
    }

    final var results$ = this.simulationService.get(planId, revisionData).map(SimulationResultsHandle::getSimulationResults);
    final var simStartTime = results$.isPresent() ? results$.get().startTime : plan.startTimestamp.toInstant();
    final var simDuration = results$.isPresent() ?
        results$.get().duration :
        Duration.of(
          plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
          Duration.MICROSECONDS);
    final var simOffset = Duration.of(plan.startTimestamp.toInstant().until(simStartTime, ChronoUnit.MICROS), Duration.MICROSECONDS);

    final var activities = new ArrayList<ActivityInstance>();
    final var simulatedActivities = results$
        .map(r -> r.simulatedActivities)
        .orElseGet(Collections::emptyMap);
    for (final var entry : simulatedActivities.entrySet()) {
      final var id = entry.getKey();
      final var activity = entry.getValue();

      final var activityOffset = Duration.of(
          simStartTime.until(activity.start(), ChronoUnit.MICROS),
          Duration.MICROSECONDS);

      activities.add(new ActivityInstance(
          id.id(),
          activity.type(),
          activity.arguments(),
          Interval.between(activityOffset, activityOffset.plus(activity.duration()))));
    }
    final var _discreteProfiles = results$
        .map(r -> r.discreteProfiles)
        .orElseGet(Collections::emptyMap);
    final var _realProfiles = results$
        .map(r -> r.realProfiles)
        .orElseGet(Collections::emptyMap);

    final var externalDatasets = this.planService.getExternalDatasets(planId);
    final var realExternalProfiles = new HashMap<String, LinearProfile>();
    final var discreteExternalProfiles = new HashMap<String, DiscreteProfile>();

    for (final var pair: externalDatasets) {
      final var offsetFromSimulationStart = pair.getLeft().minus(simOffset);
      final var profileSet = pair.getRight();

      for (final var profile: profileSet.discreteProfiles().entrySet()) {
        discreteExternalProfiles.put(profile.getKey(), DiscreteProfile.fromExternalProfile(offsetFromSimulationStart, profile.getValue().getRight()));
      }
      for (final var profile: profileSet.realProfiles().entrySet()) {
        realExternalProfiles.put(profile.getKey(), LinearProfile.fromExternalProfile(offsetFromSimulationStart, profile.getValue().getRight()));
      }
    }

    final var environment = new EvaluationEnvironment(realExternalProfiles, discreteExternalProfiles);

    final var realProfiles = new HashMap<String, LinearProfile>();
    final var discreteProfiles = new HashMap<String, DiscreteProfile>(_discreteProfiles.size());

    final var violations = new ArrayList<Violation>();
    for (final var entry : constraintCode.entrySet()) {

      // Pipeline switch
      // To remove the old constraints pipeline, delete the `useNewConstraintPipeline` variable
      // and the else branch of this if statement.
      final var constraint = entry.getValue();
      final Expression<List<Violation>> expression;

      // TODO: cache these results
      final var constraintCompilationResult = constraintsDSLCompilationService.compileConstraintsDSL(
          plan.missionModelId,
          Optional.of(planId),
          constraint.definition()
      );

      if (constraintCompilationResult instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success success) {
        expression = success.constraintExpression();
      } else if (constraintCompilationResult instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error error) {
        throw new Error("Constraint compilation failed: " + error);
      } else {
        throw new Error("Unhandled variant of ConstraintsDSLCompilationResult: " + constraintCompilationResult);
      }

      final var names = new HashSet<String>();
      expression.extractResources(names);

      final var newNames = new HashSet<String>();
      for (final var name : names) {
        if (!realProfiles.containsKey(name) && !discreteProfiles.containsKey(name)) {
          newNames.add(name);
        }
      }

      if (!newNames.isEmpty()) {
        final var newProfiles = getProfiles(results$.get(), newNames);

        for (final var _entry : ProfileSet.unwrapOptional(newProfiles.realProfiles()).entrySet()) {
          if (!realProfiles.containsKey(_entry.getKey())) {
            realProfiles.put(_entry.getKey(), LinearProfile.fromSimulatedProfile(_entry.getValue().getRight()));
          }
        }

        for (final var _entry : ProfileSet.unwrapOptional(newProfiles.discreteProfiles()).entrySet()) {
          if (!discreteProfiles.containsKey(_entry.getKey())) {
            discreteProfiles.put(_entry.getKey(), DiscreteProfile.fromSimulatedProfile(_entry.getValue().getRight()));
          }
        }
      }

      final var preparedResults = new gov.nasa.jpl.aerie.constraints.model.SimulationResults(
          simStartTime,
          Interval.between(Duration.ZERO, simDuration),
          activities,
          realProfiles,
          discreteProfiles);

      final var violationEvents = new ArrayList<Violation>();
      try {
        violationEvents.addAll(expression.evaluate(preparedResults, environment));
      } catch (final InputMismatchException ex) {
        // @TODO Need a better way to catch and propagate the exception to the
        // front end and to log the evaluation failure. This is captured in AERIE-1285.
      }


      if (violationEvents.isEmpty()) continue;

      /* TODO: constraint.evaluate returns an List<Violations> with a single empty unpopulated Violation
          which prevents the above condition being sufficient in all cases. A ticket AERIE-1230 has been
          created to account for refactoring and removing the need for this condition. */
      if (violationEvents.size() == 1 && violationEvents.get(0).violationWindows.isEmpty()) continue;

      violationEvents.forEach(violation -> violations.add(new Violation(
          entry.getValue().name(),
          entry.getKey(),
          entry.getValue().type(),
          violation.activityInstanceIds,
          new ArrayList<>(names),
          violation.violationWindows,
          violation.gaps)));
    }

    return violations;
  }

  public ProfileSet getProfiles(final gov.nasa.jpl.aerie.merlin.driver.SimulationResults simulationResults, final Iterable<String> profileNames) {
    final var realProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>>();
    for (final var profileName : profileNames) {
      if (simulationResults.realProfiles.containsKey(profileName)) {
        realProfiles.put(profileName, simulationResults.realProfiles.get(profileName));
      } else if (simulationResults.discreteProfiles.containsKey(profileName)) {
        discreteProfiles.put(profileName, simulationResults.discreteProfiles.get(profileName));
      }
    }
    return ProfileSet.of(realProfiles, discreteProfiles);
  }
}
