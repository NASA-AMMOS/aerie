package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.Violation;
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

  public List<Violation> getViolations(final PlanId planId, final Optional<SimulationDatasetId> simulationDatasetId)
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

    final var constraintSize = constraintCode.size();
    final var previouslyResolvedConstraints = this.constraintService.getPreviouslyResolvedConstraints(constraintCode.values().stream().toList());
    final var violations = new HashMap<Long, Violation>();

    // Remove any constraints that we've already checked, so they aren't rechecked.
    for (ConstraintRunRecord constraintRun : previouslyResolvedConstraints.values()) {
      constraintCode.remove(constraintRun.constraintId());
      violations.put(constraintRun.constraintId(), constraintRun.violation());
    }

    // If the lengths don't match we need check the left-over constraints.
    if (previouslyResolvedConstraints.size() != constraintSize) {
      final var resultsHandle$ = this.simulationService.get(planId, revisionData);
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

      final var externalDatasets = this.planService.getExternalDatasets(planId);
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
          final var newProfiles = resultsHandle$
              .map($ -> $.getProfiles(new ArrayList<>(newNames)))
              .orElse(ProfileSet.of(Map.of(), Map.of()));

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

        violationEvents.forEach(violation -> {
          final var newViolation = new Violation(
              entry.getValue().name(),
              entry.getKey(),
              entry.getValue().type(),
              violation.activityInstanceIds,
              new ArrayList<>(names),
              violation.violationWindows,
              violation.gaps);

          violations.put(entry.getKey(), newViolation);
        });
      }

      constraintService.createConstraintRuns(constraintCode, violations, null);
    }

    return violations.values().stream().toList();
  }
}
