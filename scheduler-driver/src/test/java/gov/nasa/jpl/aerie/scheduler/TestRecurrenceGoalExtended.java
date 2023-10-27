package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.goals.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.scheduler.SimulationUtility.buildProblemFromFoo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRecurrenceGoalExtended {

  /**
   * This test checks that a number activities are placed in the plan. VV
   */
  @Test
  public void testRecurrence() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var problem = buildProblemFromFoo(planningHorizon);
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();


    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(1, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(6, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(11, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(16, Duration.SECONDS), activityType));
    assertEquals(5, problem.getSimulationFacade().countSimulationRestarts());
  }

  /**
   * This test checks that only one activity is placed as the second one would exceed the goal window and the plan horizon. VV
   */
  @Test
  public void testRecurrenceSecondGoalOutOfWindowAndPlanHorizon() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var problem = buildProblemFromFoo(planningHorizon);
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(18, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();


    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(1, Duration.SECONDS), activityType) && (plan.getActivities().size() == 1));
    assertEquals(2, problem.getSimulationFacade().countSimulationRestarts());
  }

  /**
   * This test checks that in case the repeat interval is larger than the window where the goal can be placed, the scheduler still manages to place one activity. VV
   */
  @Test
  public void testRecurrenceRepeatIntervalLargerThanGoalWindow() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var problem = buildProblemFromFoo(planningHorizon);
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(20, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();


    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(1, Duration.SECONDS), activityType));
    assertEquals(2, problem.getSimulationFacade().countSimulationRestarts());
  }

  /**
   * This test checks that in case the window where the goal can be placed is larger than the plan horizon, then the window is updated to the plan horizon size. xx
   */
  @Test
  public void testGoalWindowLargerThanPlanHorizon() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(5),TestUtility.timeFromEpochSeconds(15));
    final var problem = buildProblemFromFoo(planningHorizon);
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    final var goalWindow = new Windows(false).set(List.of(
        Interval.between(Duration.of(1, Duration.SECONDS), Duration.of(5, Duration.SECONDS)),
        Interval.between(Duration.of(8, Duration.SECONDS), Duration.of(15, Duration.SECONDS)),
        Interval.between(Duration.of(17, Duration.SECONDS), Duration.of(20, Duration.SECONDS))
    ), true);
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(goalWindow))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();


    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(1, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(8, Duration.SECONDS), activityType));
    assertEquals(3, problem.getSimulationFacade().countSimulationRestarts());
  }


  /**
   * This test checks that in case the goal duration is larger than goal window, no activity is added to the plan. VV
   */
  @Test
  public void testGoalDurationLargerGoalWindow() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var problem = buildProblemFromFoo(planningHorizon);
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(25, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(30, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();


    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.emptyPlan(plan));
    assertEquals(1, problem.getSimulationFacade().countSimulationRestarts());
  }


  /**
   * This test checks that in case the goal repeat cycle is shorter than the goal duration, then no activity is added to the plan. VV
   */
  @Test
  public void testGoalDurationLargerRepeatInterval() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var problem = buildProblemFromFoo(planningHorizon);
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(10, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();


    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.emptyPlan(plan));
    assertEquals(1, problem.getSimulationFacade().countSimulationRestarts());
  }


  /**
   * This test checks the behaviour when activity is added to a non-empty plan. How is distance preserved? What happens if activity already existing is deleted?
   */
  @Test
  public void testAddActivityNonEmptyPlan() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var problem = buildProblemFromFoo(planningHorizon);
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();
    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution().orElseThrow();

    // Create a new problem with previous plan and add new goal interleaved two time units wrt original goal
    final var problem2 = buildProblemFromFoo(planningHorizon);
    problem2.setInitialPlan(plan);
    RecurrenceGoal goal2 = new RecurrenceGoal.Builder()
        .named("Test recurrence goal 2")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(2, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityExpression.Builder()
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .withinPlanHorizon(planningHorizon)
        .build();
    problem2.setGoals(List.of(goal2));
    final var solver2 = new PrioritySolver(problem2);
    var newplan = solver2.getNextSolution().orElseThrow();

    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(0, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(5, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(10, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(15, Duration.SECONDS), activityType));
    assertEquals(5, problem.getSimulationFacade().countSimulationRestarts());
  }
}
