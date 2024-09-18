package gov.nasa.jpl.aerie.scheduler.solver.planner;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.ActivityReference;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplate;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetworkTemporalConstraint;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.scheduler.DirectiveIdGenerator;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityNetworkConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingDecompositionConflict;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSatisfaction;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSolverResult;
import gov.nasa.jpl.aerie.scheduler.solver.Solver;
import gov.nasa.jpl.aerie.scheduler.solver.SubSolver;
import gov.nasa.jpl.aerie.scheduler.solver.stn.TaskNetworkAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
    ConflictSolverResult solverResult;

    return solveMissingDecompositionConflict((MissingDecompositionConflict) conflict);
  }

  private ConflictSolverResult solveMissingDecompositionConflict(final MissingDecompositionConflict conflict)
  throws SchedulingInterruptedException, InstantiationException
  {
    ConflictSolverResult solverResults = new ConflictSolverResult();
    assert conflict != null;
    assert plan != null;

    logger.info("Starting decomposition conflict resolution " + conflict.toString());
    final var actRef =
        conflict.getActivityType();
    List<TaskNetTemplate> candidateDecompositions =
        problem.getSchedulerModel().getMethods().get(actRef.activityType());

    if (candidateDecompositions != null && !candidateDecompositions.isEmpty()) {
      final ActivityType at = problem.getActivityType(actRef.activityType());
      final var instantiatedActivity =
          at.getSpecType().getInputType().instantiate(actRef.parameters());
      final var decompositionNet =
          candidateDecompositions.getFirst().generateTemplate(instantiatedActivity);

      plan.addTaskNetTemplateData(decompositionNet);

      solverResults = new ConflictSolverResult(
          ConflictSatisfaction.SAT,
          List.of(),
          List.of(decompositionNet)
      );
      solverResults.mergeConflictSolverResult(this.solveDependencyConflict(null,
                                                                           this.makeActivityNetworkConflict(conflict,
                                                                                                                  decompositionNet)));
      return solverResults;
    } else {
      return new ConflictSolverResult(
          ConflictSatisfaction.NOT_SAT,
          List.of(),
          new ArrayList<>()
      );
    }
  }

  public Optional<TaskNetTemplateData> getDecompositionNetwork(ActivityReference actRef) throws InstantiationException {
    List<TaskNetTemplate> candidateDecompositions =
        problem.getSchedulerModel().getMethods().get(actRef.activityType());
    //get(((MissingDecompositionConflict)conflict).getActivityType());

    if (candidateDecompositions != null && !candidateDecompositions.isEmpty()) {
      final ActivityType at = problem.getActivityType(actRef.activityType());
      //TODO jd uncommnent
      /*final var instantiatedActivity =
          at.getSpecType().getInputType().instantiate(actRef.parameters());
      return Optional.ofNullable(candidateDecompositions.getFirst().generateTemplate(instantiatedActivity));*/
    }
    return Optional.empty();
  }

  public Conflict makeActivityNetworkConflict(MissingDecompositionConflict conflict,
      TaskNetTemplateData decomposition) throws SchedulingInterruptedException
  {

    return new MissingActivityNetworkConflict(
        null,
        conflict.getEvaluationEnvironment(),
        this.constructTaskNetwork(conflict, decomposition).get(),
        this.constructMap(decomposition),
        //TODO jd uncommnet activitiesToSchedule,
        null,
        //TODO Adrien: how to get the windows:
        //new Windows(false).set(Interval.betweenClosedOpen(missingRecurrenceConflict.validWindow.start,
        // Windows(false).set(Interval.betweenClosedOpen(conflict.startInterval.start(), conflict.endInterval.end())
        new Windows(false).set(Interval.betweenClosedOpen(conflict.startInterval.start, conflict.endInterval.end),
                               true)
    );
  }

  public Optional<TaskNetworkAdapter> constructTaskNetwork(MissingDecompositionConflict conflict,
                                                           TaskNetTemplateData taskNetTemplateData)
  throws SchedulingInterruptedException
  {
    final var taskNetworkAdapter = new TaskNetworkAdapter(this.problem.getPlanningHorizon().getEndAerie());
    final var activitiesToSchedule = new ArrayList<String>();
    final var allActivitiesInNetwork = new ArrayList<String>();


    //add all activities first
    //throw error if one activity not added to a constraint
    //throw error if more than one root or orphans
    //call orderNodesRootToLeaf
    // call TaskNetwork.getOrder() to get tasks
    // add absolute starts using conflcit startInterval and call       tnw.addStartInterval(actName, startInterval.start, startInterval.end);


    HashMap<ActivityReference, String> mapRefName = new HashMap<>();
    final long nbActivities = taskNetTemplateData.subtasks().size();

    String activity1Name, activity2Name;
    final AtomicInteger idx = new AtomicInteger(1);

    // Add tasks to network
    for (ActivityReference actRef : taskNetTemplateData.subtasks()){
      activity1Name = "act" + idx.getAndIncrement();
      taskNetworkAdapter.addAct(activity1Name);
      mapRefName.put(actRef, activity1Name);
      activitiesToSchedule.add(activity1Name);
    }

    if (activitiesToSchedule.isEmpty()) {
      return Optional.empty();
    }

    // Add constraints to network
    for (TaskNetworkTemporalConstraint constraint : taskNetTemplateData.constraints()) {
      if (constraint instanceof TaskNetworkTemporalConstraint.Meets meetsConstraint) {
        ActivityReference activity1 = meetsConstraint.activity1();
        activity1Name = mapRefName.get(activity1);
        activitiesToSchedule.add(activity1Name);
        allActivitiesInNetwork.add(activity1Name);

        ActivityReference activity2 = meetsConstraint.activity2();
        activity2Name = mapRefName.get(activity2);
        activitiesToSchedule.add(activity2Name);
        allActivitiesInNetwork.add(activity2Name);

        //TODO jd remove comment taskNetworkAdapter.meets(activity1Name, activity2Name);

        final var propagationWentOkay = taskNetworkAdapter.solveConstraints();
        if (!propagationWentOkay) return Optional.empty();
      }
    }

    // If graph is not fully ordered or has cycles, throw an exception
    if(!taskNetworkAdapter.isFullyOrdered())
      throw new SchedulingInterruptedException("Graph is not fully ordered");
    if(taskNetworkAdapter.hasCycle())
      throw new SchedulingInterruptedException("Graph has cycles");


    List<String> orderedTasks = taskNetworkAdapter.getOrderedTasks();
    taskNetworkAdapter.addStartInterval(orderedTasks.getFirst(), conflict.startInterval.start,
                                        conflict.startInterval.start);
    taskNetworkAdapter.addEndInterval(orderedTasks.getLast(), conflict.endInterval.end, conflict.endInterval.end);
    return Optional.of(taskNetworkAdapter);
  }

  public HashMap<String, MissingActivityNetworkConflict.ActivityDef> constructMap(TaskNetTemplateData taskNetTemplateData){
    int idx = 1;
    final var map = new HashMap<String, MissingActivityNetworkConflict.ActivityDef>();
    for (final var actRef : taskNetTemplateData.subtasks()) {
      final var activityName = "act" + idx++;
      final var activityTemplateBuilder = new ActivityExpression.Builder()
          .ofType(problem.getActivityType(actRef.activityType()));
          //TODO jd .withSerializedArguments(actRef.parameters());
      final var activityTemplate = activityTemplateBuilder.build();
      map.put(activityName, new MissingActivityNetworkConflict.ActivityDef(activityTemplate));
    }
    return map;
  }


  public ConflictSolverResult solveDependencyConflict(Optional<Goal> goal, Conflict conflict)
  throws SchedulingInterruptedException, InstantiationException{
    return this.dependentSolver.resolveConflict(goal, conflict);
  }

}

