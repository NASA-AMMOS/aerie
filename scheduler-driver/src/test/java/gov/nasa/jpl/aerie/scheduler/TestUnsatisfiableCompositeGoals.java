package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.goals.CardinalityGoal;
import gov.nasa.jpl.aerie.scheduler.goals.ChildCustody;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.goals.CompositeAndGoal;
import gov.nasa.jpl.aerie.scheduler.goals.OptionGoal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUnsatisfiableCompositeGoals {

  private final static PlanningHorizon h = new PlanningHorizon(
      TimeUtility.fromDOY("2025-001T00:00:00.000"),
      TimeUtility.fromDOY("2025-002T00:00:00.000")
  );

  private final static Duration t0 = h.getStartAerie();
  private final static Duration d1min = Duration.of(1, Duration.MINUTE);
  private final static Duration d1hr = Duration.of(1, Duration.HOUR);
  private final static Duration t1hr = t0.plus(d1hr);
  private final static Duration t2hr = t0.plus(d1hr.times(2));

  //test mission with two primitive activity types
  private static Problem makeTestMissionAB() {
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    return new Problem(fooMissionModel, h, new SimulationFacade(h, fooMissionModel), SimulationUtility.getFooSchedulerModel());
  }

  private static PlanInMemory makePlanA12(Problem problem) {
    final var plan = new PlanInMemory();
    final var actTypeA = problem.getActivityType("ControllableDurationActivity");
    plan.add(SchedulingActivityDirective.of(actTypeA, t1hr, d1min, null, true));
    plan.add(SchedulingActivityDirective.of(actTypeA, t2hr, d1min, null, true));
    return plan;
  }

  //activities without parameters
  public CoexistenceGoal BForEachAGoal(ActivityType A, ActivityType B){

    return new CoexistenceGoal.Builder()
        .named("g0")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .forEach(new ActivityExpression.Builder()
                     .ofType(A)
                     .build())
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(B)
                            .build())
        .startsAt(TimeAnchor.START)
        .aliasForAnchors("hi I'm an alias")
        .withinPlanHorizon(h)
        .build();
  }

  @Test
  public void testAndWithoutBackTrack(){
    final var problem = makeTestMissionAB();
    problem.setInitialPlan(makePlanA12(problem));
    final var actTypeControllable = problem.getActivityType("ControllableDurationActivity");
    final var actTypeBasic = problem.getActivityType("BasicActivity");
    final var actTypeBar = problem.getActivityType("bar");
    final var goal = BForEachAGoal(actTypeControllable, actTypeBasic);
    final var goal2 = BForEachAGoal(actTypeControllable, actTypeBar);
    final var compositeAndGoal = new CompositeAndGoal.Builder()
        .and(goal2)
        .and(goal)
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .withinPlanHorizon(TestUnsatisfiableCompositeGoals.h)
        .build();
    final var exclusionBasic = TestUtility.createExclusionSchedulingZone(actTypeBasic,
                                                                         new Windows(
                                                                             Segment.of(Interval.FOREVER, false),
                                                                             Segment.of(Interval.between(t1hr.minus(d1min), t1hr.plus(d1min)), true)
                                                                         ));
    problem.add(exclusionBasic);
    problem.setGoals(List.of(compositeAndGoal));
    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().orElseThrow();
    //AND goal is unsatisfiable because of exclusion zone for the Basic activities
    //by default, goals are not backtracking so the plan should show 2 BAR activities and 1 basic activity and the 2 controllable acts from the base plan
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t1hr, actTypeControllable));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeControllable));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t1hr, actTypeBar));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeBar));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeBasic));
    Assertions.assertEquals(plan.getActivities().size(), 5);
    assertEquals(4, problem.getSimulationFacade().countSimulationRestarts());
  }

  @Test
  public void testAndWithBackTrack(){
    final var problem = makeTestMissionAB();
    problem.setInitialPlan(makePlanA12(problem));
    final var actTypeControllable = problem.getActivityType("ControllableDurationActivity");
    final var actTypeBasic = problem.getActivityType("BasicActivity");
    final var actTypeBar = problem.getActivityType("bar");
    final var goal = BForEachAGoal(actTypeControllable, actTypeBar);
    final var goal2 = BForEachAGoal(actTypeControllable, actTypeBasic);
    final var compositeAndGoal = new CompositeAndGoal.Builder()
        .and(goal2)
        .and(goal)
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .shouldRollbackIfUnsatisfied(true)
        .withinPlanHorizon(TestUnsatisfiableCompositeGoals.h)
        .build();
    final var exclusionBasic = TestUtility.createExclusionSchedulingZone(actTypeBasic,
                                                                         new Windows(
                                                                             Segment.of(Interval.FOREVER, false),
                                                                             Segment.of(Interval.between(t1hr.minus(d1min), t1hr.plus(d1min)), true)
                                                                         ));
    problem.add(exclusionBasic);
    problem.setGoals(List.of(compositeAndGoal));
    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().orElseThrow();
    //AND goal is unsatisfiable because of exclusion zone for the Basic activities
    //and goal is backtracking here so the plan should show only the 2 controllable acts from the base plan
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t1hr, actTypeControllable));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeControllable));
    Assertions.assertEquals(plan.getActivities().size(), 2);
    assertEquals(2, problem.getSimulationFacade().countSimulationRestarts());
  }

  @Test
  public void testOrWithoutBacktrack(){
    final var problem = makeTestMissionAB();
    problem.setInitialPlan(makePlanA12(problem));
    final var actTypeControllable = problem.getActivityType("ControllableDurationActivity");
    final var actTypeBasic = problem.getActivityType("BasicActivity");
    final var actTypeBar = problem.getActivityType("bar");
    final var goal = BForEachAGoal(actTypeControllable, actTypeBasic);
    final var goal2 = BForEachAGoal(actTypeControllable, actTypeBar);
    final var orGoal = new OptionGoal.Builder()
        .or(goal2)
        .or(goal)
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .withinPlanHorizon(TestUnsatisfiableCompositeGoals.h)
        .build();
    final var exclusionBasic = TestUtility.createExclusionSchedulingZone(actTypeBasic,
                                                                         new Windows(
                                                                             Segment.of(Interval.FOREVER, false),
                                                                             Segment.of(Interval.between(t1hr.minus(d1min), t1hr.plus(d1min)), true)
                                                                         ));
    final var exclusionBar = TestUtility.createExclusionSchedulingZone(actTypeBar,
                                                                       new Windows(
                                                                           Segment.of(Interval.FOREVER, false),
                                                                           Segment.of(Interval.between(t1hr.minus(d1min), t1hr.plus(d1min)), true)
                                                                       ));
    problem.add(exclusionBasic);
    problem.add(exclusionBar);
    problem.setGoals(List.of(orGoal));
    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().orElseThrow();
    //OR goal is unsatisfiable because of exclusion zone for both the Basic and Bar activities
    //goal is not configured to backtrack plan should show 2 controllable acts from the base plan + the two activities that could be scheduled out of the exclusion zone
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t1hr, actTypeControllable));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeControllable));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeBar));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeBasic));
    Assertions.assertEquals(plan.getActivities().size(), 4);
    assertEquals(3, problem.getSimulationFacade().countSimulationRestarts());
  }

  @Test
  public void testOrWithBacktrack(){
    final var problem = makeTestMissionAB();
    problem.setInitialPlan(makePlanA12(problem));
    final var actTypeControllable = problem.getActivityType("ControllableDurationActivity");
    final var actTypeBasic = problem.getActivityType("BasicActivity");
    final var actTypeBar = problem.getActivityType("bar");
    final var goal = BForEachAGoal(actTypeControllable, actTypeBasic);
    final var goal2 = BForEachAGoal(actTypeControllable, actTypeBar);
    final var orGoal = new OptionGoal.Builder()
        .or(goal2)
        .or(goal)
        .shouldRollbackIfUnsatisfied(true)
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(h.getHor(), true)))
        .withinPlanHorizon(TestUnsatisfiableCompositeGoals.h)
        .build();
    final var exclusionBasic = TestUtility.createExclusionSchedulingZone(actTypeBasic,
                                                                         new Windows(Interval.between(t1hr.minus(d1min), t1hr.plus(d1min)),
                                                                                     true));
    final var exclusionBar = TestUtility.createExclusionSchedulingZone(actTypeBar,
                                                                         new Windows(Interval.between(t1hr.minus(d1min), t1hr.plus(d1min)),
                                                                                     true));
    problem.add(exclusionBasic);
    problem.add(exclusionBar);
    problem.setGoals(List.of(orGoal));
    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().orElseThrow();
    //OR goal is unsatisfiable beacuse of exclusion zone for the Basic and Bar activities
    //goals are backtracking so the plan should show only 2 controllable acts from the base plan
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t1hr, actTypeControllable));
    Assertions.assertTrue(TestUtility.activityStartingAtTime(plan, t2hr, actTypeControllable));
    Assertions.assertEquals(plan.getActivities().size(), 2);
    assertEquals(1, problem.getSimulationFacade().countSimulationRestarts());
  }

  @Test
  public void testCardinalityBacktrack() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochSeconds(20));

    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    Problem problem = new Problem(fooMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        fooMissionModel), SimulationUtility.getFooSchedulerModel());
    final var activityType = problem.getActivityType("ControllableDurationActivity");

    final var goalWindow = new Windows(false).set(List.of(
        Interval.between(Duration.of(1, Duration.SECONDS), Duration.of(1, Duration.SECONDS)),
        Interval.between(Duration.of(7, Duration.SECONDS), Duration.of(8, Duration.SECONDS)), //exclusive
        Interval.between(Duration.of(11, Duration.SECONDS), Duration.of(13, Duration.SECONDS))
    ), true);

    //unsatisfiable because 10 occurences asked when only one can be placed
    CardinalityGoal unsatisfiableGoal = new CardinalityGoal.Builder()
        .duration(Interval.between(Duration.of(16, Duration.SECONDS), Duration.of(21, Duration.SECONDS)))
        .occurences(new Range<>(10, 10))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(problem.getActivityType("ControllableDurationActivity"))
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(new WindowsWrapperExpression(goalWindow))
        .owned(ChildCustody.Jointly)
        .shouldRollbackIfUnsatisfied(true)
        .withinPlanHorizon(TestUnsatisfiableCompositeGoals.h)
        .build();


    TestUtility.createAutoMutexGlobalSchedulingCondition(activityType).forEach(problem::add);
    problem.setGoals(List.of(unsatisfiableGoal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertEquals(0, plan.getActivities().size());
    assertEquals(2, problem.getSimulationFacade().countSimulationRestarts());
  }
}
