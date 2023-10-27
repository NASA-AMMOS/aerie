package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.goals.CardinalityGoal;
import gov.nasa.jpl.aerie.scheduler.goals.ChildCustody;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.scheduler.SimulationUtility.buildProblemFromFoo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCardinalityGoal {

  @Test
  public void testone() {
    Interval period = Interval.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(20, Duration.SECONDS));

    final var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochSeconds(25));
    final var problem = buildProblemFromFoo(planningHorizon);


    CardinalityGoal goal = new CardinalityGoal.Builder()
        .duration(Interval.between(Duration.of(12, Duration.SECONDS), Duration.of(15, Duration.SECONDS)))
        .occurences(new Range<>(3, 10))
        .thereExistsOne(new ActivityExpression.Builder()
                            .ofType(problem.getActivityType("ControllableDurationActivity"))
                            .durationIn(Duration.of(2, Duration.SECONDS))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(period, true)))
        .owned(ChildCustody.Jointly)
        .withinPlanHorizon(planningHorizon)
        .build();

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    assertTrue(plan.get().getActivitiesByTime().size() == 6);
    assertEquals(plan.get().getActivitiesByTime().stream()
                     .map(SchedulingActivityDirective::duration)
                     .reduce(Duration.ZERO, Duration::plus), Duration.of(12, Duration.SECOND));
    assertEquals(7, problem.getSimulationFacade().countSimulationRestarts());
  }
}
