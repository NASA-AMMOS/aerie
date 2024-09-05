package gov.nasa.jpl.aerie.scheduler.newsolver.solver.planner;

import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingDecompositionActivityInstantiationConflict;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.newsolver.solver.SolverDecomposer;
import gov.nasa.jpl.aerie.scheduler.newsolver.solver.scheduler.PrioritySolver;
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
public class NexusDecomposer extends SolverDecomposer {

  private static final Logger logger = LoggerFactory.getLogger(PrioritySolver.class);

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
  public Optional<Plan> resolveConflict(final Problem problem, Plan plan, Conflict conflict, boolean analysisOnly) throws
                                                                                                                   SchedulingInterruptedException
  {

    assert conflict != null;
    assert plan != null;
    this.removeConflict = false;

    //continue creating activities as long as goal wants more and we can do so
    logger.info("Starting decomposition conflict resolution " + conflict.toString());

    if(!analysisOnly && conflict instanceof MissingDecompositionActivityInstantiationConflict decompositionConflict){
      List<TaskNetTemplateData> candidateDecompositions =
          problem.getSchedulerModel().getMethods().get(decompositionConflict.getActivityReference().activityType());
      //TODO The selection of the decomposing tasknet will be done in the future by means of a heuristic
      assert candidateDecompositions != null;
      TaskNetTemplateData decompositionNet = candidateDecompositions.getFirst();
      plan.addTaskNetTemplateData(decompositionNet);
      this.removeConflict = true;
    }
    return Optional.of(plan);
  }
}
