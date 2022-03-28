package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * prototype scheduling algorithm that schedules activities for a plan
 *
 * this prototype is a single-shot priority-ordered greedy scheduler
 *
 * (note that there are many other possible scheduling algorithms!)
 */
public class PrioritySolver implements Solver {

  private static final Logger logger = LoggerFactory.getLogger(PrioritySolver.class);

  /**
   * create a new greedy solver for the specified input planning problem
   *
   * the solver is configured to operate on a given planning problem, which
   * must not change out from under the solver during its lifetime
   *
   * @param config IN, STORED controlling configuration for the solver,
   *     which must not change
   * @param problem IN, STORED description of the planning problem to be
   *     solved, which must not change
   */
  public PrioritySolver(HuginnConfiguration config, Problem problem) {
    checkNotNull(config, "creating solver with null configuration");
    checkNotNull(problem, "creating solver with null input problem descriptor");
    this.checkSimBeforeInsertingActivities = false;
    this.config = config;
    this.problem = problem;
    this.simulationFacade = problem.getSimulationFacade();
  }

  //TODO: should probably be part of sched configuration; maybe even per rule
  public void checkSimBeforeInsertingActInPlan(){
    this.checkSimBeforeInsertingActivities = true;
  }

  /**
   * {@inheritDoc}
   *
   * calculates the single-shot greedy solution to the input problem
   *
   * this solver is expended after one solution request; all subsequent
   * requests will return no solution
   */
  public Optional<Plan> getNextSolution() {
    if (plan == null) {
      //on first call to solver; setup fresh solution workspace for problem
      try {
        initializePlan();
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

  private boolean checkAndInsertAct(ActivityInstance act){
    return checkAndInsertActs(List.of(act));
  }

  /**
   * Tries to insert a collection of activity instances in plan. Simulates each of the activity and checks whether the expected
   * duration is equal to the simulated duration.
   * @param acts the activities to insert in the plan
   * @return false if at least one activity has a simulated duration not equal to the expected duration, true otherwise
   */
  private boolean checkAndInsertActs(Collection<ActivityInstance> acts){
    boolean allGood = true;

    for(var act: acts){
      //if some parameters are left uninstantiated, this is the last moment to do it
      act.instantiateVariableArguments();
      var duration = act.getDuration();
      if(duration != null && duration.longerThan(this.config.getHorizon().getEndAerie())){
        logger.warn("Activity " + act
                           + " is planned to finish after the end of the planning horizon, not simulating. Extend the planning horizon.");
        allGood = false;
        break;
      }
      if(checkSimBeforeInsertingActivities) {
        try {
          simulationFacade.simulateActivity(act);
        } catch (SimulationFacade.SimulationException e) {
          allGood = false;
          logger.error("Tried to simulate {} but the activity could not be instantiated", act, e);
          break;
        }
        var simDur = simulationFacade.getActivityDuration(act);
        if (!simDur.isPresent()) {
          logger.error("Activity " + act + " could not be simulated");
          allGood = false;
          break;
        }
        if (act.getDuration() == null) {
          act.setDuration(simDur.get());
        } else if (simDur.get().compareTo(act.getDuration()) != 0) {
          allGood = false;
          logger.error("When simulated, activity " + act
                             + " has a different duration than expected (exp=" + act.getDuration() + ", real=" + simDur + ")");
          break;
        }
      }
    }

    if(allGood) {
      //update plan with regard to simulation
      for(var act: acts) {
        plan.add(act);
      }
    } else{
      //update simulation with regard to plan

      try {
        simulationFacade.removeActivitiesFromSimulation(acts);
      } catch (SimulationFacade.SimulationException e) {
        // We do not expect to get SimulationExceptions from re-simulating activities that have been simulated before
        throw new Error("Simulation failed after removing activities");
      }

    }
    return allGood;
  }

  /**
   * creates internal storage space to build up partial solutions in
   **/
  public void initializePlan() throws SimulationFacade.SimulationException {
    plan = new PlanInMemory();

    //turn off simulation checking for initial plan contents (must accept user input regardless)
    final var prevCheckFlag = this.checkSimBeforeInsertingActivities;
    this.checkSimBeforeInsertingActivities = false;
    problem.getInitialPlan().getActivitiesByTime().stream()
      .filter( act -> (act.getStartTime()==null)
               || config.getHorizon().contains( act.getStartTime() ) )
      .forEach(this::checkAndInsertAct);
    this.checkSimBeforeInsertingActivities = prevCheckFlag;

    evaluation = new Evaluation();
    plan.addEvaluation(evaluation);

    //if backed by real models, initialize the simulation states/resources/profiles for the plan so state queries work
    if (problem.getMissionModel() != null) {
      simulationFacade.simulateActivities(plan.getActivities());
    }
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
  private void solve() {
    //construct a priority sorted goal container
    final var goalQ = getGoalQueue();
    assert goalQ != null;

    //process each goal independently in that order
    while (!goalQ.isEmpty()) {
      var goal = goalQ.remove();
      assert goal != null;

      //update the output solution plan directly to satisfy goal
      satisfyGoal(goal);
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

    //create queue container using comparator and pre-sized for all goals
    final var capacity = rawGoals.size();
    assert capacity >= 0;

    //fill the comparator-imbued container with goals to get sorted queue
    final var goalQ = new LinkedList<>(rawGoals);
    assert goalQ.size() == rawGoals.size();

    return goalQ;
  }

  private void satisfyGoal(Goal goal) {
    if (goal instanceof CompositeAndGoal) {
      satisfyCompositeGoal((CompositeAndGoal) goal);
    } else if (goal instanceof OptionGoal) {
      satisfyOptionGoal((OptionGoal) goal);
    } else {
      satisfyGoalGeneral(goal);
    }
  }


private void satisfyOptionGoal(OptionGoal goal) {
  if (goal.namongp.isSingleton() && goal.namongp.getMaximum() == 1) {
    if (goal.optimizer != null) {
      //try to satisfy all and see what is best
      Goal currentSatisfiedGoal = null;
      Collection<ActivityInstance> actsToInsert = null;
      Collection<ActivityInstance> actsToAssociateWith = null;
      for (var subgoal : goal.getSubgoals()) {
        satisfyGoal(subgoal);
        if(evaluation.forGoal(subgoal).getScore() == 0 || subgoal.isPartiallySatisfiable()) {
          var associatedActivities = evaluation.forGoal(subgoal).getAssociatedActivities();
          var insertedActivities = evaluation.forGoal(subgoal).getInsertedActivities();
          var aggregatedActivities = new ArrayList<ActivityInstance>();
          aggregatedActivities.addAll(associatedActivities);
          aggregatedActivities.addAll(insertedActivities);
          if (aggregatedActivities.size() > 0 &&
              (goal.optimizer.isBetterThanCurrent(aggregatedActivities) ||
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
        for(var act: actsToAssociateWith){
          //we do not care about ownership here as it is not really a piggyback but just the validation of the supergoal
          evaluation.forGoal(goal).associate(act, false);
        }
        if(checkAndInsertActs(actsToInsert)) {
          for(var act: actsToInsert){
            evaluation.forGoal(goal).associate(act, false);
          }
          evaluation.forGoal(goal).setScore(0);
        } else{
          //this should not happen because we have already tried to insert the same set of activities in the plan and it
          //did not failed
          throw new IllegalStateException("Had satisfied subgoal but (1) simulation or (2) association with supergoal failed");
        }
      } else {
        //number of subgoals needed to achieve supergoal
        evaluation.forGoal(goal).setScore(goal.namongp.getMaximum() - goal.namongp.getMinimum());
      }
    } else {
      //just satisfy any goal
      for (var subgoal : goal.getSubgoals()) {
        satisfyGoal(subgoal);
        //if partially satisfiability
        if (evaluation.forGoal(subgoal).getScore() == 0 || subgoal.isPartiallySatisfiable()) {
          //decision-making here : we stop at the first subgoal satisfied
          evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getAssociatedActivities(), false);
          evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getInsertedActivities(), false);
          evaluation.forGoal(goal).setScore(0);
          break;
        }
      }
      }
    } else {
      throw new IllegalArgumentException(
          "Other options than singleton namongp of OptionGoal has not yet been implemented");
    }

  }

  private void rollback(Goal goal){
    var evalForGoal = evaluation.forGoal(goal);
    var associatedActivities = evalForGoal.getAssociatedActivities();
    var insertedActivities = evalForGoal.getInsertedActivities();
    plan.remove(insertedActivities);
    evalForGoal.removeAssociation(associatedActivities);
    evalForGoal.removeAssociation(insertedActivities);
    evalForGoal.setScore(-(evalForGoal.getNbConflictsDetected().get()));
  }

  private void satisfyCompositeGoal(CompositeAndGoal goal) {
    assert goal != null;
    assert plan != null;

    boolean failed = false;

    for (var subgoal : goal.getSubgoals()) {
      satisfyGoal(subgoal);
      if (evaluation.forGoal(subgoal).getScore() != 0 && !subgoal.isPartiallySatisfiable()) {
        failed = true;
        break;
      }
    }

    if (failed) {
      //remove all activities
      for (var subgoal : goal.getSubgoals()) {
        rollback(subgoal);
      }
    } else{
      for(var subgoal : goal.getSubgoals()) {
        evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getAssociatedActivities(), false);
        evaluation.forGoal(goal).associate(evaluation.forGoal(subgoal).getInsertedActivities(), false);
        evaluation.forGoal(goal).setScore(0);
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
  private void satisfyGoalGeneral(Goal goal) {

    assert goal != null;
    assert plan != null;

    //continue creating activities as long as goal wants more and we can do so
    var missingConflicts = getMissingConflicts(goal);
    //setting the number of conflicts detected at first evaluation, will be used at backtracking
    evaluation.forGoal(goal).setNbConflictsDetected(missingConflicts.size());
    assert missingConflicts != null;
    boolean madeProgress = true;
    while (!missingConflicts.isEmpty() && madeProgress) {
      madeProgress = false;

      //create new activity instances for each missing conflict
      for (final var missing : missingConflicts) {
        assert missing != null;

        //determine the best activities to satisfy the conflict
        if (missing instanceof MissingActivityInstanceConflict || missing instanceof MissingActivityTemplateConflict) {
          final var acts = getBestNewActivities((MissingActivityConflict) missing);
          assert acts != null;
          //add the activities to the output plan
          if (!acts.isEmpty()) {
            var actsCanBeInserted = checkAndInsertActs(acts);
            if(actsCanBeInserted){
              madeProgress = true;

              evaluation.forGoal(goal).associate(acts, true);
              //REVIEW: really association should be via the goal's own query...

              //NB: repropagation of new activity effects occurs on demand
              //    at next constraint query, if relevant
            }
          }
        } else if(missing instanceof MissingAssociationConflict missingAssociationConflict){
          var actToChooseFrom = missingAssociationConflict.getActivityInstancesToChooseFrom();
          //no act type constraint to consider as the activities have been scheduled
          //no global constraint for the same reason above mentioned
          //only the target goal state constraints to consider
          for(var act : actToChooseFrom){
            var actWindow = new Windows();
            actWindow.add(Window.between(act.getStartTime(), act.getEndTime()));
            var stateConstraints = goal.getStateConstraints();
            var narrowed = actWindow;
            if(stateConstraints!= null) {
              narrowed = narrowByStateConstraints(actWindow, List.of(stateConstraints));
            }
            if(narrowed.includes(actWindow)){
              //decision-making here, we choose the first satisfying activity
              evaluation.forGoal(goal).associate(act, false);
              madeProgress = true;
              break;
            }
          }
        }
      }//for(missing)

      if (madeProgress) {
        missingConflicts = getMissingConflicts(goal);
      }
    }//while(missingConflicts&&madeProgress)

    if(missingConflicts.size() > 0 && !goal.isPartiallySatisfiable()){
      rollback(goal);
    } else{
      evaluation.forGoal(goal).setScore(-missingConflicts.size());
    }
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
  private Collection<Conflict>
  getMissingConflicts(Goal goal)
  {
    assert goal != null;
    assert plan != null;

    //find all the reasons this goal is crying
    //REVIEW: maybe should have way to request only certain kinds of conflicts
    final var rawConflicts = goal.getConflicts(plan);
    assert rawConflicts != null;

    //filter out any issues that this simple algorithm can't deal with (ie
    //anything that doesn't just require throwing more instances in the plan)
    final var filteredConflicts = new LinkedList<Conflict>();
    for (final var conflict : rawConflicts) {
      assert conflict != null;
      //if( conflict instanceof MissingActivityConflict ) {
      filteredConflicts.add(conflict);
      //}
    }

    return filteredConflicts;
  }

  /**
   * determines the best activity instances to add to improve the plan
   *
   * calculates the scheduling for a set of activity instances that will best
   * satisfy the given conflict in the fixed context of the current plan (but
   * does not actually put them in the solution yet)
   *
   * the suggested activities might only reduce the degree of conflict present
   * without eliminating it completely
   *
   * multiple activities may be returned, eg to allow for scheduling
   * interelated activities (eg co-dependent observations, ancillary
   * setup/cleanups, etc)
   *
   * //REVIEW: should multiple acts be handled by a decomposition instead?
   *
   * the activities are chosen in a myopic greedy fashion: they ignore any
   * opportunity cost of choices on subsequent goal satisfaction (either for
   * their own or others goals). the algorithm also does not consider other
   * joint modifications to the plan (eg moves to allow scheduling)
   *
   * this method does at least choose additions that avoid introducing any new
   * conflicts with anything that is already in the plan, including registered
   * state constraints
   *
   * returns an empty container if there are no activities that can be added
   * to satisfy the conflict without introducing other conflicts
   *
   * the output plan member must exist and be valid
   *
   * @param missing IN the conflict describing an acute lack of an activity
   *     that is causing goal dissatisfaction in the current plan
   * @return an ensemble of new activity instances that are suggested to be
   *     added to the plan to best satisfy the conflict without disrupting
   *     the rest of the plan, or null if there are no such suggestions
   */
  private Collection<ActivityInstance>
  getBestNewActivities(MissingActivityConflict missing)
  {
    assert missing != null;
    var newActs = new LinkedList<ActivityInstance>();

    //REVIEW: maybe push into polymorphic method of conflict/goal? (picking best act
    //may depend on the source goal)
    final var goal = missing.getGoal();

    //start from the time window where the missing activity causes a problem
    //NB: these are start windows
    var possibleWindows = new Windows(missing.getTemporalContext());

    //prune based on constraints on goal and activity type (mutex, state,
    //event, etc)
    //TODO: move this into polymorphic method. don't want to be demuxing types
    Collection<StateConstraintExpression> stateConstraints = new LinkedList<>();

    //add all goal constraints
    StateConstraintExpression goalConstraints = goal.getStateConstraints();
    ActivityType actType;

    if (goalConstraints != null) {
      stateConstraints.add(goalConstraints);
    }
    if (missing instanceof final MissingActivityInstanceConflict missingInstance) {
      final var act = missingInstance.getInstance();
      actType = act.getType();
      StateConstraintExpression c = act.getType().getStateConstraints();
      if (c != null) stateConstraints.add(c);
    } else if (goal instanceof ActivityTemplateGoal) {
      StateConstraintExpression c = ((ActivityTemplateGoal) goal).getActivityStateConstraints();
      if (c != null) stateConstraints.add(c);
    } else {
      //TODO: placeholder for now to avoid mutex fall through
      throw new IllegalArgumentException("request to create activities for conflict of unrecognized type");
    }
    possibleWindows = narrowByStateConstraints(possibleWindows, stateConstraints);

    possibleWindows = narrowGlobalConstraints(plan, missing, possibleWindows, this.problem.getGlobalConstraints());

    //narrow to windows where activity duration will fit
    final var startWindows = possibleWindows;
    //for now handling just start-time windows, so no need to prune duration
    //    //REVIEW: how to handle dynamic durations? for now pessimistic!
    //    final var durationMax = goal.getActivityDurationRange().getMaximum();
    //    possibleWindows = null;
    //    startWindows.contractBy( Duration.ofZero(), durationMax );

    //create new act if there is any valid time (otherwise conflict is
    //unsatisfiable in current plan)
    if (!startWindows.isEmpty()) {
      //TODO: move this into a polymorphic method? definitely don't want to be
      //demuxing on all the conflict types here
      if (missing instanceof final MissingActivityInstanceConflict missingInstance) {
        //FINISH: clean this up code dupl re windows etc
        final var act = missingInstance.getInstance();
        newActs.add(new ActivityInstance(act));
//        final var windows = createWindows( act, startWindows );
//        newActs.addAll( windows );
      } else if (missing instanceof final MissingActivityTemplateConflict missingTemplate) {
        //select the "best" time among the possibilities, and latest among ties
        //REVIEW: currently not handling preferences / ranked windows
        final var startT = startWindows.minTimePoint();

        //create the new activity instance (but don't place in schedule)
        //REVIEW: not yet handling multiple activities at a time
        final var template = missingTemplate.getActTemplate();
        final var completeTemplate = new ActivityCreationTemplate.Builder()
            .basedOn(template).startsIn(Window.between(startT.get(), startT.get())).build();
        final var act = completeTemplate.createActivity(
            goal.getName() + "_" + java.util.UUID.randomUUID());
        if (act != null) {
          newActs.add(act);
        }//if(act)
      }

    }//if(startWindows)

    return newActs;
  }

  /**
   * contracts the given windows according to the provided constraints
   *
   * the remaining windows after this call will be the interesection of
   * all the individual constraint windows with the initial input windows
   *
   * evaluates the constraints in the context of the current solution plan
   *
   * the remaining windows may be empty!
   *
   * @param windows IN/OUT the windows to be contracted by constraints.
   *     updated in place. may be empty (but not null)
   * @param constraints IN the constraints to use to narrow the windows,
   *     may be empty (but not null)
   */
  private Windows narrowByStateConstraints(
      Windows windows, Collection<StateConstraintExpression> constraints)
  {
    assert windows != null;
    assert constraints != null;
    Windows ret = new Windows(windows);
    //short circuit on already empty windows or no constraints: no work to do!
    if (windows.isEmpty() || constraints.isEmpty()) {
      return ret;
    }

    //REVIEW: could be some optimization in constraint ordering

    //iteratively narrow the windows from each constraint
    for (final var constraint : constraints) {
      ret = constraint.findWindows(plan, ret);
      assert ret != null;
      //short-circuit if no possible windows left
      if (windows.isEmpty()) {
        break;
      }
    }
  return ret;
  }

  private Windows narrowGlobalConstraints(
      Plan plan,
      MissingActivityConflict mac,
      Windows windows,
      Collection<GlobalConstraint> constraints) {
    Windows tmp = new Windows(windows);
    for (GlobalConstraint gc : constraints) {
      if (gc instanceof BinaryMutexConstraint) {
        tmp = ((BinaryMutexConstraint) gc).findWindows(plan, tmp, mac);
      } else if (gc instanceof NAryMutexConstraint){
        tmp = ((NAryMutexConstraint) gc).findWindows(plan, tmp, mac);
      }
    }
  return tmp;
  }

  public void printEvaluation() {
    logger.warn("Remaining conflicts for goals ");
    for (var goalEval : evaluation.getGoals()) {
      logger.warn(goalEval.name + " -> " + evaluation.forGoal(goalEval).score);
      logger.warn("Activities created by this goal:"+  evaluation.forGoal(goalEval).getInsertedActivities().stream().map(ActivityInstance::toString).collect(
          Collectors.joining(" ")));
      logger.warn("Activities associated to this goal:"+  evaluation.forGoal(goalEval).getAssociatedActivities().stream().map(ActivityInstance::toString).collect(
          Collectors.joining(" ")));
    }
  }

  boolean checkSimBeforeInsertingActivities;

  /**
   * the controlling configuration for the solver
   *
   * remains constant throughout solver lifetime
   */
  final HuginnConfiguration config;

  /**
   * description of the planning problem to solve
   *
   * remains constant throughout solver lifetime
   */
  final Problem problem;

  /**
   * the single-shot priority-ordered greedy solution devised by the solver
   *
   * this object is null until first call to getNextSolution()
   */
  Plan plan;

  /**
   * tracks how well this solver thinks it has satisfied goals
   *
   * including which activities were created to satisfy each goal
   */
  Evaluation evaluation;

  private final SimulationFacade simulationFacade;

}
