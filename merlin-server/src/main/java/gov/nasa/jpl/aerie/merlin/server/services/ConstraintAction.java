package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationDatasetId;
import gov.nasa.jpl.aerie.merlin.server.models.SimulationResultsHandle;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.models.ProfileSet;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConstraintAction {
  private final ConstraintsDSLCompilationService constraintsDSLCompilationService;
  private final ConstraintService constraintService;
  private final PlanService planService;
  private final MissionModelService missionModelService;
  private final SimulationService simulationService;

  public ConstraintAction(
      final ConstraintsDSLCompilationService constraintsDSLCompilationService,
      final ConstraintService constraintService,
      final PlanService planService,
      final MissionModelService missionModelService,
      final SimulationService simulationService
  ) {
    this.constraintsDSLCompilationService = constraintsDSLCompilationService;
    this.constraintService = constraintService;
    this.planService = planService;
    this.missionModelService = missionModelService;
    this.simulationService = simulationService;
  }

  public List<ConstraintResult> getViolations(final PlanId planId, final Optional<SimulationDatasetId> simulationDatasetId)
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

    final var resultsHandle$ = this.simulationService.get(planId, revisionData);
    final var simDatasetId = simulationDatasetId.orElseGet(() -> resultsHandle$
        .map(SimulationResultsHandle::getSimulationDatasetId)
        .orElse(null));
    final var violations = new HashMap<Long, ConstraintResult>();

    if (simDatasetId != null) {
      final var validConstraintRuns = this.constraintService.getValidConstraintRuns(constraintCode.values().stream().toList(), simDatasetId);

      // Remove any constraints that we've already checked, so they aren't rechecked.
      for (ConstraintRunRecord constraintRun : validConstraintRuns.values()) {
        constraintCode.remove(constraintRun.constraintId());

        if (constraintRun.violation() != null) {
          violations.put(constraintRun.constraintId(), constraintRun.violation());
        }
      }
    }

    // If the lengths don't match we need check the left-over constraints.
    if (!constraintCode.isEmpty()) {
      final var simStartTime = resultsHandle$
          .map(gov.nasa.jpl.aerie.merlin.server.models.SimulationResultsHandle::startTime)
          .orElse(plan.startTimestamp.toInstant());
      final var simDuration = resultsHandle$
          .map(SimulationResultsHandle::duration)
          .orElse(Duration.of(
              plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
              Duration.MICROSECONDS));
      final var simOffset = Duration.of(
          plan.startTimestamp.toInstant().until(simStartTime, ChronoUnit.MICROS),
          Duration.MICROSECONDS);

      final var activities = new ArrayList<ActivityInstance>();
      final var simulatedActivities = resultsHandle$
          .map(SimulationResultsHandle::getSimulatedActivities)
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

      final var externalDatasets = this.planService.getExternalDatasets(planId, simulationDatasetId);
      final var realExternalProfiles = new HashMap<String, LinearProfile>();
      final var discreteExternalProfiles = new HashMap<String, DiscreteProfile>();

      for (final var pair : externalDatasets) {
        final var offsetFromSimulationStart = pair.getLeft().minus(simOffset);
        final var profileSet = pair.getRight();

        for (final var profile : profileSet.discreteProfiles().entrySet()) {
          discreteExternalProfiles.put(
              profile.getKey(),
              DiscreteProfile.fromExternalProfile(
                  offsetFromSimulationStart,
                  profile.getValue().getRight()));
        }
        for (final var profile : profileSet.realProfiles().entrySet()) {
          realExternalProfiles.put(
              profile.getKey(),
              LinearProfile.fromExternalProfile(
                  offsetFromSimulationStart,
                  profile.getValue().getRight()));
        }
      }

      final var environment = new EvaluationEnvironment(realExternalProfiles, discreteExternalProfiles);

      final var realProfiles = new HashMap<String, LinearProfile>();
      final var discreteProfiles = new HashMap<String, DiscreteProfile>();

      for (final var entry : constraintCode.entrySet()) {
        final var constraint = entry.getValue();
        final Expression<ConstraintResult> expression;

        // TODO: cache these results, @JoelCourtney is this in reference to caching the output of the DSL compilation?
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
          final var newProfiles = resultsHandle$
              .map($ -> $.getProfiles(new ArrayList<>(newNames)))
              .orElseThrow(() -> new InputMismatchException("no simulation results found for plan id " + planId));

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

        ConstraintResult constraintResult = expression.evaluate(preparedResults, environment);

        if (constraintResult.isEmpty()) continue;

        constraintResult.constraintName = entry.getValue().name();
        constraintResult.constraintId = entry.getKey();
        constraintResult.constraintType = entry.getValue().type();
        constraintResult.resourceIds = List.copyOf(names);

        violations.put(entry.getKey(), constraintResult);
      }

      if (simDatasetId != null) {
        constraintService.createConstraintRuns(
            constraintCode,
            violations,
            simDatasetId);
      }
    }

    return violations.values().stream().toList();
  }
}
