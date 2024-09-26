package gov.nasa.jpl.aerie.scheduler.solver;

import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.Problem;

import java.util.Optional;

/**
 * interface to a scheduling algorithm that produces schedules for input plans
 *
 * depending on the kind of solver and its configuration, the algorithm may
 * produce one or many different solutions to the same planning problem: eg by
 * iteratively improving on a solution, by providing different high level
 * options, etc
 */
public abstract class SubSolver {
  protected ConflictSatisfaction satisfaction;
  protected Solver metaSolver;
  protected SubSolver dependentSolver;

  /**
   * calculates the next solution plan based on solver configuration
   *
   * operates according to the solver's controlling configuration to determine
   * a new solution, which may range from an iterative improvement on a prior
   * solution up to a contrasting high level alternative for consideration
   *
   * in general, the solution trajectory is not guaranteed to be identical
   * across different executions on the same problem, though some specific
   * algorithms may optionally support deterministic solution trajectories
   *
   * in general, the solver method is not re-entrant safe, though some
   * specific algorithms may optionally support parallel solution requests
   *
   * in general, the solver requires that any previously specified input
   * planning problem descriptor and solver configuration remain unchanged
   * between successive calls, though some algorithms may optionally support
   * changing problem specification
   *
   * the algorithm may return no solution in the event the solver has
   * expended all of its solutions (eg by reaching a requested quality
   * threshold, proving an optimum solution, exhausting the configured
   * alternative search space, etc). in general, the solver will return no
   * solution for all requests thereafter, but some algorithms may optionally
   * support further solutions (eg on some input/configuration modification)
   */
  public abstract ConflictSolverResult resolveConflict(Plan plan, Optional<Goal> goal, Conflict conflict)
  throws SchedulingInterruptedException, InstantiationException;

  public void setDependentSolver(SubSolver solver) {
    this.dependentSolver = solver;
  }

  public abstract ConflictSolverResult solveDependencyConflict(Optional<Goal> goal, Conflict conflict)
  throws SchedulingInterruptedException, InstantiationException;

}

