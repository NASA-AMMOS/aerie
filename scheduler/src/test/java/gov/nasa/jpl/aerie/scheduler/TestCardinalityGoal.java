package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.goals.CardinalityGoal;
import gov.nasa.jpl.aerie.scheduler.goals.ChildCustody;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestCardinalityGoal {

  @Test
  public void testone() {
    Window period = Window.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(20, Duration.SECONDS));

    var periodTre = new TimeRangeExpression.Builder()
        .from(new Windows(period))
        .build();
    ActivityType actType = new ActivityType("CardGoalActType", null, DurationType.controllable("duration"));


    CardinalityGoal goal = new CardinalityGoal.Builder()
        .inPeriod(periodTre)
        .duration(Window.between(Duration.of(12, Duration.SECONDS), Duration.of(15, Duration.SECONDS)))
        .occurences(new Range<>(3, 10))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actType)
                            .duration(Duration.of(2, Duration.SECONDS))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(period)
        .owned(ChildCustody.Jointly)
        .build();

    Problem problem = new Problem(null, new PlanningHorizon(TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochSeconds(25)), null, null);

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);
    var plan = solver.getNextSolution();
    assert(plan.get().getActivitiesByTime().size() == 6);
    assertEquals(plan.get().getActivitiesByTime().stream()
                     .map(ActivityInstance::getDuration)
                     .reduce(Duration.ZERO, Duration::plus), Duration.of(12, Duration.SECOND));
  }


  @Test
  public void unsatifiablegoaltest() {

    Window period = Window.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(10, Duration.SECONDS));
    Window period2 = Window.betweenClosedOpen(Duration.of(13, Duration.SECONDS), Duration.of(20, Duration.SECONDS));

    var periodTre = new TimeRangeExpression.Builder()
        .from(new Windows(List.of(period, period2)))
        .build();

    ActivityType actType = new ActivityType("CardGoalActType", null, DurationType.controllable("duration"));

    assertThrows(IllegalArgumentException.class, () -> {new CardinalityGoal.Builder()
        .inPeriod(periodTre)
        .duration(Window.between(Duration.of(5, Duration.SECONDS), Duration.of(6, Duration.SECONDS)))
        .occurences(new Range<>(3, 10))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actType)
                            .duration(Window.between(Duration.of(3, Duration.SECONDS), Duration.of(4, Duration.SECONDS)))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(period)
        .owned(ChildCustody.Jointly)
        .build();});
  }

}
