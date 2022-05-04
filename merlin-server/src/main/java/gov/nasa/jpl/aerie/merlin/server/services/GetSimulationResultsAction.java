package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.json.ConstraintParsers;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.merlin.driver.SimulationResults;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import javax.json.Json;
import java.io.StringReader;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GetSimulationResultsAction {
  public /*sealed*/ interface Response {
    record Pending() implements Response {}
    record Incomplete() implements Response {}
    record Failed(String reason) implements Response {}
    record Complete(SimulationResults results, Map<String, List<Violation>> violations) implements Response {}
  }

  private final PlanService planService;
  private final MissionModelService missionModelService;
  private final SimulationService simulationService;
  private final ConstraintsDSLCompilationService constraintsDSLCompilationService;
  private final boolean useNewConstraintPipeline;

  public GetSimulationResultsAction(
      final PlanService planService,
      final MissionModelService missionModelService,
      final SimulationService simulationService,
      final ConstraintsDSLCompilationService constraintsDSLCompilationService,
      final boolean useNewConstraintPipeline
  ) {
    this.planService = Objects.requireNonNull(planService);
    this.missionModelService = Objects.requireNonNull(missionModelService);
    this.simulationService = Objects.requireNonNull(simulationService);
    this.constraintsDSLCompilationService = Objects.requireNonNull(constraintsDSLCompilationService);
    this.useNewConstraintPipeline = useNewConstraintPipeline;
  }

  public Response run(final PlanId planId) throws NoSuchPlanException {
    final var revisionData = this.planService.getPlanRevisionData(planId);

    final var response = this.simulationService.getSimulationResults(planId, revisionData);

    if (response instanceof ResultsProtocol.State.Pending) {
      return new Response.Pending();
    } else if (response instanceof ResultsProtocol.State.Incomplete) {
      return new Response.Incomplete();
    } else if (response instanceof ResultsProtocol.State.Failed r) {
      return new Response.Failed(r.reason());
    } else if (response instanceof ResultsProtocol.State.Success r) {
      final var results = r.results();
      final var violations = getViolations(planId, results);

      return new Response.Complete(results, violations);
    } else {
      throw new UnexpectedSubtypeError(ResultsProtocol.State.class, response);
    }
  }

  public Map<String, List<Violation>> getViolations(final PlanId planId, final SimulationResults results)
  throws NoSuchPlanException
  {
    final var plan = this.planService.getPlan(planId);

    final var constraintJsons = new HashMap<String, Constraint>();

    try {
      this.missionModelService.getConstraints(plan.missionModelId).forEach(
          (name, constraint) -> constraintJsons.put("model/" + name, constraint)
      );
      this.planService.getConstraintsForPlan(planId).forEach(
          (name, constraint) -> constraintJsons.put("plan/" + name, constraint)
      );
    } catch (final MissionModelService.NoSuchMissionModelException ex) {
      throw new RuntimeException("Assumption falsified -- mission model for existing plan does not exist");
    }


    final var activities = new ArrayList<ActivityInstance>();
    for (final var entry : results.simulatedActivities.entrySet()) {
      final var id = entry.getKey();
      final var activity = entry.getValue();

      final var activityOffset = Duration.of(
          plan.startTimestamp.toInstant().until(activity.start(), ChronoUnit.MICROS),
          Duration.MICROSECONDS);

      activities.add(new ActivityInstance(
          id.id(),
          activity.type(),
          activity.arguments(),
          Window.between(activityOffset, activityOffset.plus(activity.duration()))));
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

    final var planDuration = Duration.of(
        plan.startTimestamp.toInstant().until(plan.endTimestamp.toInstant(), ChronoUnit.MICROS),
        Duration.MICROSECONDS);

    final var preparedResults = new gov.nasa.jpl.aerie.constraints.model.SimulationResults(
        Window.between(Duration.ZERO, planDuration),
        activities,
        realProfiles,
        discreteProfiles);

    final var violations = new HashMap<String, List<Violation>>();
    for (final var entry : constraintJsons.entrySet()) {

      // Pipeline switch
      // To remove the old constraints pipeline, delete the `useNewConstraintPipeline` variable
      // and the else branch of this if statement.
      final var constraint = entry.getValue();
      final Expression<List<Violation>> expression;
      if (this.useNewConstraintPipeline) {

        // TODO: cache these results
        final var constraintCompilationResult = constraintsDSLCompilationService.compileConstraintsDSL(
            planId,
            constraint.definition()
        );

        if (constraintCompilationResult instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success success) {
          expression = success.constraintExpression();
        } else if (constraintCompilationResult instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error error) {
          throw new Error("Constraint compilation failed: " + error);
        } else {
          throw new Error("Unhandled variant of ConstraintsDSLCompilationResult: " + constraintCompilationResult);
        }

      } else {
        final var subject = Json.createReader(new StringReader(entry.getValue().definition())).readValue();
        final var constraintParseResult = ConstraintParsers.constraintP.parse(subject);

        if (constraintParseResult.isFailure()) {
          throw new Error(entry.getValue().definition());
        }

        expression = constraintParseResult.getSuccessOrThrow();
      }
      final var violationEvents = new ArrayList<Violation>();
      try {
        violationEvents.addAll(expression.evaluate(preparedResults));
      } catch (final InputMismatchException ex) {
        // @TODO Need a better way to catch and propagate the exception to the
        // front end and to log the evaluation failure. This is captured in AERIE-1285.
      }


      if (violationEvents.isEmpty()) continue;

      /* TODO: constraint.evaluate returns an List<Violations> with a single empty unpopulated Violation
          which prevents the above condition being sufficient in all cases. A ticket AERIE-1230 has been
          created to account for refactoring and removing the need for this condition. */
      if (violationEvents.size() == 1 && violationEvents.get(0).violationWindows.isEmpty()) continue;

      final var names = new HashSet<String>();
      expression.extractResources(names);
      final var resourceNames = new ArrayList<>(names);
      final var violationEventsWithNames = new ArrayList<Violation>();
      violationEvents.forEach(violation -> violationEventsWithNames.add(new Violation(
          violation.activityInstanceIds,
          resourceNames,
          violation.violationWindows)));

      violations.put(entry.getKey(), violationEventsWithNames);
    }

    return violations;
  }
}
