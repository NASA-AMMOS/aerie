package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.goals.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestRecurrenceGoal {

  @Test
  public void testRecurrence() {
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    Problem problem = new Problem(fooMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        fooMissionModel), SimulationUtility.getFooSchedulerModel());
    final var activityType = problem.getActivityType("ControllableDurationActivity");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(20, Duration.SECONDS)), true)))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .duration(Duration.of(2, Duration.SECONDS))
                            .ofType(activityType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .build();


    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(1, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(6, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(11, Duration.SECONDS), activityType));
    assertTrue(TestUtility.activityStartingAtTime(plan,Duration.of(16, Duration.SECONDS), activityType));
  }

  @Test
  public void testRecurrenceNegative() {
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    Problem problem = new Problem(fooMissionModel,
                                  planningHorizon,
                                  new SimulationFacade(planningHorizon,
                                                       fooMissionModel),
                                  SimulationUtility.getFooSchedulerModel());

    try {
      final var activityType = problem.getActivityType("ControllableDurationActivity");
      final var goal = new RecurrenceGoal.Builder()
          .named("Test recurrence goal")
          .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(Interval.betweenClosedOpen(Duration.of(1, Duration.SECONDS),
                                                 Duration.of(20, Duration.SECONDS)), true)))
          .thereExistsOne(new ActivityCreationTemplate.Builder()
                              .duration(Duration.of(2, Duration.SECONDS))
                              .ofType(activityType)
                              .build())
          .repeatingEvery(Duration.of(-1, Duration.SECONDS))
          .build();
    }
    catch (IllegalArgumentException e) {
      //minimum is checked first so that's the output, even though the value for repeatingEvery is set as both the min
      //    and max possible duration
      assertTrue(e.getMessage()
                  .contains("Duration passed to RecurrenceGoal as the goal's minimum recurrence interval cannot be negative!"));
    }
    catch (Exception e) {
      fail(e.getMessage());
    }
  }

}
