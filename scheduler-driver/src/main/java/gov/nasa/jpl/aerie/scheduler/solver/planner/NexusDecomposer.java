package gov.nasa.jpl.aerie.scheduler.solver.planner;

import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplate;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.scheduler.DirectiveIdGenerator;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;

import gov.nasa.jpl.aerie.scheduler.conflicts.MissingDecompositionConflict;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSatisfaction;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSolverResult;
import gov.nasa.jpl.aerie.scheduler.solver.Solver;
import gov.nasa.jpl.aerie.scheduler.solver.SubSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
   * description of the planning problem to solve
   *
   * remains constant throughout solver lifetime
   */
  private final Problem problem;

  /**
   * the single-shot priority-ordered greedy solution devised by the solver
   *
   * this object is null until first call to getNextSolution()
   */
  private Plan plan;

  /**
   * boolean stating whether only conflict analysis should be performed or not
   */
  private final boolean analysisOnly;
  private final DirectiveIdGenerator idGenerator;


  /**
   * SolverDecomposer to repair a decomposition conflict
   *
   * @param problem IN, STORED description of the planning problem to be
   *     solved, which must not change
   * @param plan IN, description of the current list of activities, constraints and tasknets
   *
   * @throws SchedulingInterruptedException
   */
  public NexusDecomposer(final Problem problem, final Plan plan, final boolean analysisOnly,
                         final DirectiveIdGenerator idGenerator, Solver metaSolver) {
    this.problem = problem;
    this.plan = plan;
    this.analysisOnly = analysisOnly;
    this.idGenerator = idGenerator;
    this.metaSolver = metaSolver;
  }

  /**
   * SolverDecomposer to repair a decomposition conflict
   *
   * @param conflict IN the conflict describing a compound activity that needs to be decomposed
   * @throws SchedulingInterruptedException
   */
  @Override
  public ConflictSolverResult resolveConflict(Optional<Goal> goal, Conflict conflict)
  throws SchedulingInterruptedException, InstantiationException
  {
    return solveMissingDecompositionConflict(conflict);
  }

  private ConflictSolverResult solveMissingDecompositionConflict(final Conflict conflict)
  throws SchedulingInterruptedException, InstantiationException
  {
    assert conflict != null;
    assert plan != null;

    logger.info("Starting decomposition conflict resolution " + conflict.toString());
    final var actRef =
        ((MissingDecompositionConflict) conflict).getActivityType();
    List<TaskNetTemplate> candidateDecompositions =
        problem.getSchedulerModel().getMethods().get(actRef.activityType());
    //get(((MissingDecompositionConflict)conflict).getActivityType());

    if (candidateDecompositions != null && !candidateDecompositions.isEmpty()) {
      final ActivityType at = problem.getActivityType(actRef.activityType());
      final var instantiatedActivity =
          at.getSpecType().getInputType().instantiate(actRef.parameters());
      final var decompositionNet =
          candidateDecompositions.getFirst().generateTemplate(instantiatedActivity);

      plan.addTaskNetTemplateData(decompositionNet);

      /*
      conflicts.add(new MissingDecompositionActivityInstantiationConflict(
          null,
          evaluationEnvironment,
          activityReference));*/

      // Create ConflictSolverResult with the decomposition and SAT status
      return new ConflictSolverResult(
          ConflictSatisfaction.SAT,
          List.of(),
          List.of(decompositionNet)
      );
    } else {
      return new ConflictSolverResult(
          ConflictSatisfaction.NOT_SAT,
          List.of(),
          new HashMap<>()
      );
    }
  }

    public ConflictSolverResult solveDependencyConflict(Optional<Goal> goal, Conflict conflict)
  throws SchedulingInterruptedException, InstantiationException{
      return this.dependentSolver.resolveConflict(goal, conflict);
  }
}

