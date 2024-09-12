package gov.nasa.jpl.aerie.merlin.server.services.constraints;

import gov.nasa.ammos.aerie.procedural.constraints.Violations;
import gov.nasa.ammos.aerie.procedural.timeline.plan.SimulationResults;
import gov.nasa.jpl.aerie.merlin.driver.MerlinToProcedureSimulationResultsAdapter;
import gov.nasa.jpl.aerie.merlin.driver.TypeUtilsPlanToProcedurePlanAdapter;
import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.exceptions.SimulationDatasetMismatchException;
import gov.nasa.jpl.aerie.merlin.server.http.Fallible;
import gov.nasa.jpl.aerie.merlin.server.models.*;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.ConstraintRunRecord;
import gov.nasa.jpl.aerie.merlin.server.services.MissionModelService;
import gov.nasa.jpl.aerie.merlin.server.services.PlanService;
import gov.nasa.jpl.aerie.merlin.server.services.SimulationService;

import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;

public class ConstraintAction {
  private final ConstraintsDSLCompilationService constraintsDSLCompilationService;
  private final ConstraintService constraintService;
  private final PlanService planService;
  private final SimulationService simulationService;

  public ConstraintAction(
      final ConstraintsDSLCompilationService constraintsDSLCompilationService,
      final ConstraintService constraintService,
      final PlanService planService,
      final SimulationService simulationService
  ) {
    this.constraintsDSLCompilationService = constraintsDSLCompilationService;
    this.constraintService = constraintService;
    this.planService = planService;
    this.simulationService = simulationService;
  }

  public Map<Constraint, Fallible<?>> getViolations(final PlanId planId, final Optional<SimulationDatasetId> simulationDatasetId)
  throws NoSuchPlanException, MissionModelService.NoSuchMissionModelException, SimulationDatasetMismatchException
  {
    final var plan = this.planService.getPlanForValidation(planId);
    final var timelinePlan = new TypeUtilsPlanToProcedurePlanAdapter(plan);
    final Optional<SimulationResultsHandle> resultsHandle;
    final SimulationResults simResults;
    final SimulationDatasetId simDatasetId;
    if (simulationDatasetId.isPresent()) {
      resultsHandle = this.simulationService.get(planId, simulationDatasetId.get());
      simDatasetId = resultsHandle
          .map(SimulationResultsHandle::getSimulationDatasetId)
          .orElseThrow(() -> new InputMismatchException("simulation dataset with id `"
                                                        + simulationDatasetId.get().id()
                                                        + "` does not exist"));
      simResults = resultsHandle
          .map(SimulationResultsHandle::getSimulationResults)
          .map($ -> new MerlinToProcedureSimulationResultsAdapter($, false, timelinePlan))
          .get();
    } else {
      final var revisionData = this.planService.getPlanRevisionData(planId);
      resultsHandle = this.simulationService.get(planId, revisionData);
      simDatasetId = resultsHandle
          .map(SimulationResultsHandle::getSimulationDatasetId)
          .orElseThrow(() -> new InputMismatchException("plan with id "
                                                        + planId.id()
                                                        + " has not yet been simulated at its current revision"));
      simResults = resultsHandle
          .map(SimulationResultsHandle::getSimulationResults)
          .map($ -> new MerlinToProcedureSimulationResultsAdapter($, false, timelinePlan))
          .get();
    }

    final var constraintCode = new HashMap<>(this.planService.getConstraintsForPlan(planId));
    final var constraintResultMap = new HashMap<Constraint, Fallible<?>>();

    final var validConstraintRuns = this.constraintService.getValidConstraintRuns(constraintCode, simDatasetId);

    // Remove any constraints that we've already checked, so they aren't rechecked.
    for (ConstraintRunRecord constraintRun : validConstraintRuns.values()) {
        constraintResultMap.put(constraintCode.remove(constraintRun.constraintId()), Fallible.of(constraintRun.result()));
    }

    // If the lengths don't match we need check the left-over constraints.
    if (!constraintCode.isEmpty()) {
      final var simDuration = resultsHandle
          .map(SimulationResultsHandle::duration)
          .orElse(plan.simulationDuration());

      // try to compile and run the constraint that were not
      // successful and cached in the past
      for (final var entry : constraintCode.entrySet()) {
        final var constraint = entry.getValue();
        final ConstraintResult result;

        final ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult constraintCompilationResult;
        try {
          constraintCompilationResult = constraintsDSLCompilationService.compileConstraintsDSL(
              plan.missionModelId(),
              Optional.of(planId),
              Optional.of(simDatasetId),
              constraint.definition()
          );
        } catch (MissionModelService.NoSuchMissionModelException | NoSuchPlanException ex) {
          constraintResultMap.put(
              constraint,
              Fallible.failure(new Error("Constraint " + constraint.name() + ": " + ex.getMessage())));
          continue;
        }

        final JsonValue json;

        // Try to compile the constraint and capture failures
        if (constraintCompilationResult instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Success success) {
          json = success.constraintExpression();
        } else if (constraintCompilationResult instanceof ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error error) {
          constraintResultMap.put(
              constraint,
              Fallible.failure(error, "Constraint '" + constraint.name() + "' compilation failed:\n "));
          continue;
        } else {
          constraintResultMap.put(
              constraint,
              Fallible.failure(
                  new ConstraintsDSLCompilationService.ConstraintsDSLCompilationResult.Error(
                      new ArrayList<>() {{
                        add(new ConstraintsCompilationError.UserCodeError(
                            "Unhandled variant of ConstraintsDSLCompilationResult: "
                            + constraintCompilationResult,
                            "",
                            new ConstraintsCompilationError.CodeLocation(
                                0,
                                0),
                            ""));
                      }})));
          continue;
        }

        final var runner = new ConstraintRunner(timelinePlan, simResults);

        Violations violations = runner.getConstraintP().parse(json).getSuccessOrThrow();

        ConstraintResult constraintResult = new ConstraintResult(violations.collect(timelinePlan.totalBounds()));

        constraintResult.setConstraintName(entry.getValue().name());
        constraintResult.setConstraintRevision(entry.getValue().revision());
        constraintResult.setConstraintId(entry.getKey());

        constraintResultMap.put(constraint, Fallible.of(constraintResult));
      }
      // Filter for constraints that were compiled and ran with results
      // convert these successful failables to ConstraintResults
      final var compiledConstraintMap = constraintResultMap.entrySet().stream()
                                                           .filter(set -> {
                                                             Fallible<?> fallible = set.getValue();
                                                             return !fallible.isFailure() && (fallible
                                                                                                  .getOptional()
                                                                                                  .isPresent()
                                                                                              && fallible
                                                                                                  .getOptional()
                                                                                                  .get() instanceof ConstraintResult);
                                                           })
                                                           .collect(Collectors.toMap(
                                                               entry -> entry.getKey().id(),
                                                               set -> (ConstraintResult) set
                                                                   .getValue()
                                                                   .getOptional()
                                                                   .get()));

      // Use the constraints that were compiled and ran with results
      // to filter out the constraintCode map to match
      final var compileConstraintCode =
          constraintCode.entrySet().stream().filter(set -> compiledConstraintMap.containsKey(set.getKey())).collect(
              Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // Only update the db when constraints were compiled and ran with results.
      constraintService.createConstraintRuns(
          compileConstraintCode,
          compiledConstraintMap,
          simDatasetId);
    }

    return constraintResultMap;
  }
}
