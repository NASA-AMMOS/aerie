package gov.nasa.jpl.aerie.scheduler.solver.planner;

import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;

import gov.nasa.jpl.aerie.scheduler.conflicts.MissingDecompositionConflict;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSatisfaction;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSolverResult;
import gov.nasa.jpl.aerie.scheduler.solver.SubSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * prototype scheduling algorithm that schedules activities for a plan
 *
 * this prototype is a single-shot priority-ordered greedy scheduler
 *
 * (note that there are many other possible scheduling algorithms!)
 */
public class NexusDecomposer extends SubSolver {

  private static final Logger logger = LoggerFactory.getLogger(NexusDecomposer.class);

  /**
   * SolverDecomposer to repair a decomposition conflict
   *
   * @param problem IN, STORED description of the planning problem to be
   *     solved, which must not change
   * @param plan IN, description of the current list of activities, constraints and tasknets
   * @param conflict IN the conflict describing a compound activity that needs to be decomposed
   * @throws SchedulingInterruptedException
   */
  @Override
  public ConflictSolverResult resolveConflict(final Problem problem, Plan plan, Conflict conflict, boolean analysisOnly) throws SchedulingInterruptedException {
    return solveMissingDecompositionConflict(problem, plan, conflict);
  }

  private ConflictSolverResult solveMissingDecompositionConflict(
      final Problem problem, final Plan plan, final Conflict conflict) throws SchedulingInterruptedException
  {
    assert conflict != null;
    assert plan != null;

    logger.info("Starting decomposition conflict resolution " + conflict.toString());

    List<TaskNetTemplateData> candidateDecompositions =
        problem.getSchedulerModel().getMethods().get(((MissingDecompositionConflict)conflict).getActivityType());

    if (candidateDecompositions != null && !candidateDecompositions.isEmpty()) {
      //TODO The selection of the decomposing tasknet will be done in the future by means of a heuristic
      TaskNetTemplateData decompositionNet = candidateDecompositions.getFirst();
      plan.addTaskNetTemplateData(decompositionNet);

      // Create ConflictSolverResult with the decomposition and SAT status
      return new ConflictSolverResult(
          ConflictSatisfaction.SAT,
          List.of(),
          List.of(decompositionNet)
      );
    }
    else{
      return new ConflictSolverResult(
          ConflictSatisfaction.NOT_SAT,
          List.of(),
          List.of()
      );
    }
  }
}
