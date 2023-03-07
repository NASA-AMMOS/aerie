package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.goals.CardinalityGoal;
import gov.nasa.jpl.aerie.scheduler.goals.ChildCustody;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCardinalityGoal {

  @Test
  public void testone() {
    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(20, Duration.SECONDS));

    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochSeconds(25));
    Problem problem = new Problem(fooMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        fooMissionModel), SimulationUtility.getFooSchedulerModel());

    CardinalityGoal goal = new CardinalityGoal.Builder()
        .duration(Interval.between(Duration.of(12, Duration.SECONDS), Duration.of(15, Duration.SECONDS)))
        .occurences(new Range<>(3, 10))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(problem.getActivityType("ControllableDurationActivity"))
                            .duration(Duration.of(2, Duration.SECONDS))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .owned(ChildCustody.Jointly)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    assertTrue(plan.get().getActivitiesByTime().size() == 6);
    assertEquals(plan.get().getActivitiesByTime().stream()
                     .map(SchedulingActivityDirective::duration)
                     .reduce(Duration.ZERO, Duration::plus), Duration.of(12, Duration.SECOND));
  }


  @Test
  public void unsatifiablegoaltest() {

    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(10, Duration.SECONDS));
    Interval period2 = Interval.betweenClosedOpen(Duration.of(13, Duration.SECONDS), Duration.of(20, Duration.SECONDS));

    final var fooMissionModel = SimulationUtility.getFooMissionModel();
    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochSeconds(25));
    Problem problem = new Problem(fooMissionModel, planningHorizon, new SimulationFacade(
        planningHorizon,
        fooMissionModel), SimulationUtility.getFooSchedulerModel());

    assertThrows(IllegalArgumentException.class, () -> {new CardinalityGoal.Builder()
        .duration(Interval.between(Duration.of(5, Duration.SECONDS), Duration.of(6, Duration.SECONDS)))
        .occurences(new Range<>(3, 10))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(problem.getActivityType("ControllableDurationActivity"))
                            .duration(Interval.between(Duration.of(3, Duration.SECONDS), Duration.of(4, Duration.SECONDS)))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .owned(ChildCustody.Jointly)
        .build();});
  }

}
