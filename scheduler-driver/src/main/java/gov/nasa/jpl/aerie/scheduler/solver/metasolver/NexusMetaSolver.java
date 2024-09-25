package gov.nasa.jpl.aerie.scheduler.solver.metasolver;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.ActivityReference;
import gov.nasa.jpl.aerie.merlin.protocol.model.htn.TaskNetTemplateData;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.scheduler.DirectiveIdGenerator;
import gov.nasa.jpl.aerie.scheduler.EquationSolvingAlgorithms;
import gov.nasa.jpl.aerie.scheduler.SchedulingInterruptedException;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingAssociationConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingDecompositionConflict;
import gov.nasa.jpl.aerie.scheduler.goals.CompositeAndGoal;
import gov.nasa.jpl.aerie.scheduler.goals.Goal;
import gov.nasa.jpl.aerie.scheduler.goals.OptionGoal;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivity;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationData;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSatisfaction;
import gov.nasa.jpl.aerie.scheduler.solver.ConflictSolverResult;
import gov.nasa.jpl.aerie.scheduler.solver.Evaluation;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import gov.nasa.jpl.aerie.scheduler.solver.Solver;
import gov.nasa.jpl.aerie.scheduler.solver.planner.NexusDecomposer;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * prototype scheduling algorithm that schedules activities for a plan
 *
 * this prototype is a single-shot priority-ordered greedy scheduler
 *
 * (note that there are many other possible scheduling algorithms!)
 */
public class NexusMetaSolver implements Solver {

  private static final Logger logger = LoggerFactory.getLogger(NexusMetaSolver.class);

  private boolean checkSimBeforeInsertingActivities;

  private boolean checkSimBeforeEvaluatingGoal;

  private boolean atLeastOneSimulateAfter;

  private SimulationData cachedSimulationResultsBeforeGoalEvaluation;
  private final NexusDecomposer decomposer;
  private final PrioritySolver scheduler;

  /**
   * boolean stating whether only conflict analysis should be performed or not
   */
  private final boolean analysisOnly;

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

  private final SimulationFacade simulationFacade;

  private final DirectiveIdGenerator idGenerator;


  public record ActivityMetadata(SchedulingActivity activityDirective){}

  public static class HistoryWithActivity implements EquationSolvingAlgorithms.History<Duration, ActivityMetadata> {
    List<Pair<EquationSolvingAlgorithms.FunctionCoordinate<Duration>, Optional<ActivityMetadata>>> events;

    public HistoryWithActivity(){
      events = new ArrayList<>();
    }
    public void add(EquationSolvingAlgorithms.FunctionCoordinate<Duration> functionCoordinate, ActivityMetadata activityMetadata){
      this.events.add(Pair.of(functionCoordinate, Optional.ofNullable(activityMetadata)));
    }

    @Override
    public List<Pair<EquationSolvingAlgorithms.FunctionCoordinate<Duration>, Optional<ActivityMetadata>>> getHistory() {
      return events;
    }

    public Optional<Pair<EquationSolvingAlgorithms.FunctionCoordinate<Duration>, Optional<ActivityMetadata>>> getLastEvent(){
      if(events.isEmpty()) return Optional.empty();
      return Optional.of(events.get(events.size() - 1));
    }

    @Override
    public boolean alreadyVisited(final Duration x) {
      for(final var event:events){
        if(event.getLeft().x().isEqualTo(x)) return true;
      }
      return false;
    }

    public void logHistory(){
      logger.info("Rootfinding history");
      for(final var event: events){
        logger.info("Start:" + event.getLeft().x() + " end:" + (event.getLeft().fx()==null ? "error" : event.getLeft().fx()));
      }
    }
  }

  /**
   * create a new greedy solver for the specified input planning problem
   *
   * the solver is configured to operate on a given planning problem, which
   * must not change out from under the solver during its lifetime
   *
   * @param problem IN, STORED description of the planning problem to be
   *     solved, which must not change
   */
  public NexusMetaSolver(final Problem problem, final boolean analysisOnly) {
    checkNotNull(problem, "creating solver with null input problem descriptor");
    this.checkSimBeforeInsertingActivities = true;
    this.checkSimBeforeEvaluatingGoal = true;
    this.atLeastOneSimulateAfter = false;
    this.problem = problem;
    this.simulationFacade = problem.getSimulationFacade();
    this.analysisOnly = analysisOnly;
    this.idGenerator = new DirectiveIdGenerator(
        problem
            .getInitialPlan()
            .getActivitiesById()
            .keySet()
            .stream()
            .map(ActivityDirectiveId::id)
            .max(Long::compareTo)
            .orElse(-1L)
        + 1
    );
    this.decomposer = new NexusDecomposer(this.problem, this.plan, this.analysisOnly, this.idGenerator, this);
    //TODO jd check if that's ok to pass problem here
    this.scheduler = new PrioritySolver(this.problem, this.plan, this.analysisOnly, this.idGenerator, this);

    this.decomposer.setDependentSolver(scheduler);
    this.scheduler.setDependentSolver(decomposer);
  }

  public NexusMetaSolver(final Problem problem) {
    this(problem, false);
  }

  /**
   * {@inheritDoc}
   *
   * calculates the single-shot greedy solution to the input problem
   *
   * this solver is expended after one solution request; all subsequent
   * requests will return no solution
   */
  public Optional<Plan> getNextSolution() throws SchedulingInterruptedException, InstantiationException {
    if (plan == null) {
      //on first call to solver; setup fresh solution workspace for problem
      if(simulationFacade.getCanceledListener().get()) throw new SchedulingInterruptedException("initializing plan");
      try {
        initializePlan();
        if(problem.getInitialSimulationResults().isPresent()) {
          logger.debug("Loading initial simulation results from the DB");
          simulationFacade.setInitialSimResults(problem.getInitialSimulationResults().get());
        }
      } catch (SimulationFacade.SimulationException e) {
        logger.error("Tried to initializePlan but at least one activity could not be instantiated", e);
        return Optional.empty();
      }

      //attempt to satisfy the goals in the problem
      solve();

      return Optional.of(plan);

    } else { //plan!=null

      //subsequent call after initial solution, so return null
      //(this simple solver only produces a single solution)
      return Optional.empty();
    }
  }

  public record InsertActivityResult(boolean success, List<SchedulingActivity> activitiesInserted){}

  /**
   * Tries to insert a collection of activity instances in plan. Simulates each of the activity and checks whether the expected
   * duration is equal to the simulated duration.
   * @param acts the activities to insert in the plan
   * @return false if at least one activity has a simulated duration not equal to the expected duration, true otherwise
   */
  private InsertActivityResult checkAndInsertActs(Collection<SchedulingActivity> acts) throws SchedulingInterruptedException{
    // TODO: When anchors are allowed to be added by Scheduling goals, inserting the new activities one at a time should be reconsidered
    boolean allGood = true;
    logger.info("Inserting new activities in the plan to check plan validity");
    for(var act: acts){
      //if some parameters are left uninstantiated, this is the last moment to do it
      var duration = act.duration();
      if(duration != null && act.startOffset().plus(duration).longerThan(this.problem.getPlanningHorizon().getEndAerie())) {
        logger.warn("Not simulating activity " + act
                    + " because it is planned to finish after the end of the planning horizon.");
        return new InsertActivityResult(allGood, List.of());
      }
    }
    final var planWithAddedActivities = (PlanInMemory) plan.duplicate();
    planWithAddedActivities.add(acts);
    try {
      if(checkSimBeforeInsertingActivities) simulationFacade.simulateNoResultsAllActivities(planWithAddedActivities);
      plan = planWithAddedActivities;
    } catch (SimulationFacade.SimulationException e) {
      allGood = false;
      logger.error("Tried to simulate the plan {} but a simulation exception happened", planWithAddedActivities, e);
    }
    return new InsertActivityResult(allGood, acts.stream().map(act -> plan.getActivitiesById().get(act.id())).toList());
  }


  /**
   * creates internal storage space to build up partial solutions in
   **/
  public void initializePlan() throws SimulationFacade.SimulationException, SchedulingInterruptedException {
    plan = new PlanInMemory();

    //turn off simulation checking for initial plan contents (must accept user input regardless)
    final var prevCheckFlag = this.checkSimBeforeInsertingActivities;
    this.checkSimBeforeInsertingActivities = false;
    checkAndInsertActs(problem.getInitialPlan().getActivitiesByTime());
    this.checkSimBeforeInsertingActivities = prevCheckFlag;

    plan.addEvaluation(new Evaluation());
  }


  /**
   * iteratively fills in output plan to satisfy input problem description
   *
   * calculates a single-shot priority-ordered greedy solution to the problem;
   * ie it proceeds from highest to lowest priority goal, scheduling
   * activities for each in turn to the best still-available windows. it does
   * not attempt any search between different goals or even within the same
   * goal. priority ties are broken by alphabetic ordering of goal id.
   *
   * the solution may not be optimal or even dominating: eg, it can be fooled
   * into scheduling lenient high priority goals into times that conflict with
   * much more constrained lower priority goals
   *
   * the configuration, problem, and plan members must exist and be valid
   *
   * the output plan member is updated directly with the devised solution
   */
  private void solve() throws SchedulingInterruptedException, InstantiationException {
    //construct a priority sorted goal container
    final var goalQ = getGoalQueue();
    assert goalQ != null;

    //process each goal independently in that order
    while (!goalQ.isEmpty()) {
      var goal = goalQ.remove();
      assert goal != null;

      //update the output solution plan directly to satisfy goal
      satisfyGoal(goal);
      satisfyDecompositions();
    }
  }

  /**
   * construct a priority sorted queue of goals to process
   *
   * the goals are ordered in descending priority (highest priority first)
   * with ties broken by the natural ordering of the goal identifiers
   *
   * the returned queue becomes owned by the caller, and may be modified
   * by removing or adding further goals
   *
   * the configuration and problem members must exist and be valid
   *
   * @return a descending-priority ordered queue of goals from the input
   *     problem, ready for processing
   */
  private LinkedList<Goal> getGoalQueue() {
    assert problem != null;
    final var rawGoals = problem.getGoals();
    assert rawGoals != null;

    this.atLeastOneSimulateAfter = rawGoals.stream().filter(g -> g.simulateAfter).findFirst().isPresent();

    //create queue container using comparator and pre-sized for all goals
    final var capacity = rawGoals.size();
    assert capacity >= 0;

    //fill the comparator-imbued container with goals to get sorted queue
    final var goalQ = new LinkedList<>(rawGoals);
    assert goalQ.size() == rawGoals.size();

    return goalQ;
  }

  private void satisfyDecompositions() throws SchedulingInterruptedException, InstantiationException {
    assert plan != null;

    //continue creating activities as long as goal wants more and we can do so
    logger.info("Starting conflict detection for new Task Networks");
    var missingConflicts = getConflictsDecomposition();
    logger.info("Found "+ missingConflicts.size() +" conflicts in conflict detection");
    //setting the number of conflicts detected at first evaluation, will be used at backtracking
    //TODO jd create evaulation for task networks
    //plan.getEvaluation().forGoal(goal).increaseNbConflictsDetected(missingConflicts.size());
    assert missingConflicts != null;

    final var itConflicts = missingConflicts.iterator();
    int i = 0;
    //create new activity instances for each missing conflict
    while (itConflicts.hasNext()) {
      final var missing = itConflicts.next();
      assert missing != null;
      logger.info("Processing conflict " + (++i));
      logger.info(missing.toString());
      //determine the best activities to satisfy the conflict

      if(missing instanceof MissingDecompositionConflict){
        this.decomposer.resolveConflict(null, missing);
        //TODO jd check where to remove conflict
      }
      else {
        this.scheduler.resolveConflict(null, missing);
      }
    }//for(missing)
    logger.info("Finishing decomposition satisfaction" +":"+ (missingConflicts.size() == 0 ?
                                                                  "SUCCESS" : "FAILURE. Number of conflicts that could not be addressed: " + missingConflicts.size()));
    //TODO js define score for decomposition
    //plan.getEvaluation().forGoal(goal).setScore(-missingConflicts.size());
  }

  private void satisfyGoal(Goal goal) throws SchedulingInterruptedException, InstantiationException {
    if(simulationFacade.getCanceledListener().get()) throw new SchedulingInterruptedException("satisfying goal");
    final boolean checkSimConfig = this.checkSimBeforeInsertingActivities;
    this.checkSimBeforeInsertingActivities = goal.simulateAfter;
    if (goal instanceof CompositeAndGoal compositeAndGoal) {
      satisfyCompositeGoal(compositeAndGoal);
    } else if (goal instanceof OptionGoal optionGoal) {
      satisfyOptionGoal(optionGoal);
    } else {
      satisfyGoalGeneral(goal);
    }
    this.checkSimBeforeEvaluatingGoal = goal.simulateAfter;
    this.checkSimBeforeInsertingActivities = checkSimConfig;
  }


  private void satisfyOptionGoal(OptionGoal goal) throws SchedulingInterruptedException, InstantiationException {
    if (goal.hasOptimizer()) {
      //try to satisfy all and see what is best
      Goal currentSatisfiedGoal = null;
      Collection<SchedulingActivity> actsToInsert = null;
      Collection<SchedulingActivity> actsToAssociateWith = null;
      for (var subgoal : goal.getSubgoals()) {
        satisfyGoal(subgoal);
        if(plan.getEvaluation().forGoal(subgoal).getScore() == 0 || !subgoal.shouldRollbackIfUnsatisfied()) {
          var associatedActivities = plan.getEvaluation().forGoal(subgoal).getAssociatedActivities();
          var insertedActivities = plan.getEvaluation().forGoal(subgoal).getInsertedActivities();
          var aggregatedActivities = new ArrayList<SchedulingActivity>();
          aggregatedActivities.addAll(associatedActivities);
          aggregatedActivities.addAll(insertedActivities);
          if (!aggregatedActivities.isEmpty() &&
              (goal.getOptimizer().isBetterThanCurrent(aggregatedActivities) ||
               currentSatisfiedGoal == null)) {
            actsToInsert = insertedActivities;
            actsToAssociateWith = associatedActivities;
            currentSatisfiedGoal = subgoal;
          }
        }
        rollback(subgoal);
      }
      //we should have the best solution
      if (currentSatisfiedGoal != null) {
        final var insertionResult = checkAndInsertActs(actsToInsert);
        final var goalEvaluation = plan.getEvaluation().forGoal(goal);
        if(insertionResult.success()) {
          for(var act: insertionResult.activitiesInserted()){
            goalEvaluation.associate(act, false, null);
          }
          goalEvaluation.setConflictSatisfaction(null, ConflictSatisfaction.SAT);
        } else{
          rollback(currentSatisfiedGoal);

        }
      } else {
        plan.getEvaluation().forGoal(goal).setConflictSatisfaction(null, ConflictSatisfaction.NOT_SAT);
      }
    } else {
      var atLeastOneSatisfied = false;
      //just satisfy any goal
      for (var subgoal : goal.getSubgoals()) {
        satisfyGoal(subgoal);
        final var evaluation = plan.getEvaluation();
        final var subgoalIsSatisfied = (evaluation.forGoal(subgoal).getSatisfaction() == ConflictSatisfaction.SAT);
        evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getAssociatedActivities(), false, null);
        evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getInsertedActivities(), true, null);
        if(subgoalIsSatisfied){
          logger.info("OR goal " + goal.getName() + ": subgoal " + subgoal.getName() + " has been satisfied, stopping");
          atLeastOneSatisfied = true;
          break;
        }
        logger.info("OR goal " + goal.getName() + ": subgoal " + subgoal.getName() + " could not be satisfied, moving on to next subgoal");
      }
      if(atLeastOneSatisfied){
        plan.getEvaluation().forGoal(goal).setConflictSatisfaction(null, ConflictSatisfaction.SAT);
      } else {
        plan.getEvaluation().forGoal(goal).setConflictSatisfaction(null, ConflictSatisfaction.NOT_SAT);
        if(goal.shouldRollbackIfUnsatisfied()) {
          for (var subgoal : goal.getSubgoals()) {
            rollback(subgoal);
          }
        }
      }
    }
  }

  @Override
  public void rollback(ConflictSolverResult conflictResults){
  //TODO jd complete
  }

  @Override
  public void rollback(Goal goal){
    var evalForGoal = plan.getEvaluation().forGoal(goal);
    var associatedActivities = evalForGoal.getAssociatedActivities();
    var insertedActivities = evalForGoal.getInsertedActivities();
    plan.remove(insertedActivities);
    evalForGoal.removeAssociation(associatedActivities);
    evalForGoal.removeAssociation(insertedActivities);
  }

  private void satisfyCompositeGoal(CompositeAndGoal goal) throws SchedulingInterruptedException,
                                                                  InstantiationException
  {
    assert goal != null;
    assert plan != null;

    var nbGoalSatisfied = 0;
    for (var subgoal : goal.getSubgoals()) {
      satisfyGoal(subgoal);
      if (plan.getEvaluation().forGoal(subgoal).getSatisfaction() == ConflictSatisfaction.SAT) {
        logger.info("AND goal " + goal.getName() + ": subgoal " + subgoal.getName() + " has been satisfied, moving on to next subgoal");
        nbGoalSatisfied++;
      } else {
        logger.info("AND goal " + goal.getName() + ": subgoal " + subgoal.getName() + " has NOT been satisfied");
        if(goal.shouldRollbackIfUnsatisfied()){
          logger.info("AND goal " + goal.getName() + ": stopping goal satisfaction after first failure, remove shouldRollbackIfUnsatisfied(true) on AND goal to maximize satisfaction instead of early termination");
          break;
        }
        logger.info("AND goal " + goal.getName() + ": moving on to next subgoal (trying to maximize satisfaction)");
      }
    }
    final var goalIsSatisfied = (nbGoalSatisfied == goal.getSubgoals().size());
    if (goalIsSatisfied) {
      plan.getEvaluation().forGoal(goal).setConflictSatisfaction(null, ConflictSatisfaction.SAT);
    } else {
      plan.getEvaluation().forGoal(goal).setConflictSatisfaction(null, ConflictSatisfaction.NOT_SAT);
    }

    if(!goalIsSatisfied && goal.shouldRollbackIfUnsatisfied()){
      for (var subgoal : goal.getSubgoals()) {
        rollback(subgoal);
      }
    }
    if(goalIsSatisfied) {
      for (var subgoal : goal.getSubgoals()) {
        final var evaluation = plan.getEvaluation();
        evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getAssociatedActivities(), false, null);
        evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getInsertedActivities(), true, null);
      }
    }
  }


  /**
   * attempts to satisfy the specified goal as much as possible
   *
   * updates the output plan member with newly scheduled activities in order
   * to meet the goal, but does so without perturbing any of the existing
   * scheduled activities (as required by the strict priority ordering of the
   * algorithm)
   *
   * the scheduled activities are placed in a myopic greedy fashion, utilizing
   * the best timing and parameters for each new activity and ignoring any
   * potential downstream impact on either this or subsequent goal
   * achievement. (eg it might even be fooled into blocking its own subsequent
   * activities.) in case of ties in timing selection, activities are
   * scheduled at the latest permissible slots
   *
   * the configuration, problem, and plan members must exist and be valid
   *
   * the goal must be a member of the problem specification
   *
   * @param goal IN the single goal to address with plan modifications
   */
  private void satisfyGoalGeneral(Goal goal) throws SchedulingInterruptedException, InstantiationException {
    assert goal != null;
    assert plan != null;
    //continue creating activities as long as goal wants more and we can do so
    logger.info("Starting conflict detection before goal " + goal.getName());
    // Resolve first
    var missingConflicts = getConflicts(goal);
    var missingConflictsHTN = getConflictsDecomposition();
    plan.getEvaluation().forGoal(goal).addConflicts(missingConflicts);
    logger.info("Found "+ missingConflicts.size() +" conflicts in conflict detection");
    //setting the number of conflicts detected at first evaluation, will be used at backtracking
    assert missingConflicts != null;
    final var alreadyTried = new ArrayList<Conflict>();
    int i = 0;
    final var itConflicts = missingConflicts.iterator();

    // Resolve goal conflicts
    while (itConflicts.hasNext()) {
      final var missing = itConflicts.next();
      assert missing != null;
      logger.info("Processing conflict " + (++i));
      logger.info(missing.toString());
      //determine the best activities to satisfy the conflict
      ConflictSolverResult conflictSolverReturn = null;

      this.scheduler.resolveConflict(Optional.of(goal), missing);

      if(conflictSolverReturn.satisfaction() == ConflictSatisfaction.SAT) itConflicts.remove();
      //missing association is the only one associating directly
      if(!(missing instanceof MissingAssociationConflict)){
        plan.getEvaluation().forGoal(goal).associate(conflictSolverReturn.activitiesCreated(), true, missing);
      }
      plan.getEvaluation().forGoal(goal).setConflictSatisfaction(missing, conflictSolverReturn.satisfaction());
    }

    // Resolve conflicts from non-decomposed compound SchedulingActivities added manually to the plan
    while (itConflicts.hasNext()) {
      final var missing = itConflicts.next();
      assert missing != null;
      logger.info("Processing conflict " + (++i));
      logger.info(missing.toString());
      //determine the best activities to satisfy the conflict
      ConflictSolverResult conflictSolverReturn = null;

      this.decomposer.resolveConflict(Optional.of(goal), missing);

      if(conflictSolverReturn.satisfaction() == ConflictSatisfaction.SAT) itConflicts.remove();
      //missing association is the only one associating directly
      if(!(missing instanceof MissingAssociationConflict)){
        plan.getEvaluation().forGoal(goal).associate(conflictSolverReturn.activitiesCreated(), true, missing);
      }
      plan.getEvaluation().forGoal(goal).setConflictSatisfaction(missing, conflictSolverReturn.satisfaction());
    }

    if(!missingConflicts.isEmpty() && goal.shouldRollbackIfUnsatisfied()){
      logger.warn("Rolling back changes for "+goal.getName());
      rollback(goal);
    }
    logger.info("Finishing goal satisfaction for goal " + goal.getName() +":"+ (missingConflicts.size() == 0 ? "SUCCESS" : "FAILURE. Number of conflicts that could not be addressed: " + missingConflicts.size()));
  }

  /**
   * finds plan conflicts due to missing activities induced by the goal
   *
   * the solution plan must exist and be valid
   *
   * @param goal IN the goal to find missing activities for
   * @return the set of missing activity conflicts in the current solution
   *     plan due to the specified goal
   */
  private Collection<Conflict> getConflicts(Goal goal) throws SchedulingInterruptedException
  {
    assert goal != null;
    assert plan != null;
    //REVIEW: maybe should have way to request only certain kinds of conflicts
    logger.debug("Computing simulation results until "+ this.problem.getPlanningHorizon().getEndAerie() + " (planning horizon end) in order to compute conflicts");
    final var resources = new HashSet<String>();
    goal.extractResources(resources);
    final var simulationResults = this.scheduler.getLatestSimResultsUpTo(this.problem.getPlanningHorizon().getEndAerie(),
                                                                         resources);
    final var evaluationEnvironment = new EvaluationEnvironment(this.problem.getRealExternalProfiles(), this.problem.getDiscreteExternalProfiles());
    final var rawConflicts = goal.getConflicts(
        plan,
        simulationResults.constraintsResults(),
        evaluationEnvironment,
        this.problem.getSchedulerModel());
    assert rawConflicts != null;
    return rawConflicts;
  }

  /**
   * Evaluates conflicts in the plan resulting from planning activities
   * @return
   */
  public Collection<Conflict> getConflictsDecomposition(){
    final var evaluationEnvironment = new EvaluationEnvironment(problem.getRealExternalProfiles(), problem.getDiscreteExternalProfiles());
    final var conflicts = new LinkedList<Conflict>();

    //Conflict if any directive is compound
    for(SchedulingActivity act: plan.getActivities()){
      if(act.getType().isCompound() && !act.getType().getSpecType().getDecompositionRule().isPresent()){
        //TODO jd create conflict. consider that here you don't have activityreference. think on difference between
        // activity that needs to be decomposed (here) and activity that needs to be instantiated (below)
        conflicts.add(new MissingDecompositionConflict(null, evaluationEnvironment,
                                                       act));
      }
    }

    //TODO jd check how nexusdecomposer works when conflict doesnt have start and end interval
    for(TaskNetTemplateData taskNetTemplateData : ((PlanInMemory)plan).getPendingDecompositions()) {
      for (ActivityReference activityReference : taskNetTemplateData.subtasks()) {
        if (problem.getActivityType(activityReference.activityType()).isCompound()) {
          conflicts.add(new MissingDecompositionConflict(null, evaluationEnvironment,
                                                         activityReference));
        }
      }
    }

    /*

     */

    // Conflicts for each ActivityReference from a TaskNet added as a result of decomposition:
    // 1. If activity is compound: Conflict to decompose it
    // 2. Conflict for each activity to create a SchedulingActivity from the ActivityReference
    //TODO jd implement this with code to instantiate tasknet
    /*
    for(TaskNetTemplate taskNetTemplate : plan.getDecompositions()){
      for(ActivityReference activityReference : taskNetTemplate.getNetwork().subtasks().values()){
        if(activityReference.isCompound()) {
          conflicts.add(new MissingDecompositionConflict(null, evaluationEnvironment,
                                                         taskNetTemplate.getId(), activityReference.id()));
        }
        conflicts.add(new MissingDecompositionActivityInstantiationConflict(null, evaluationEnvironment, activityReference));
      }
    }
     */
    return conflicts;
  }
}

