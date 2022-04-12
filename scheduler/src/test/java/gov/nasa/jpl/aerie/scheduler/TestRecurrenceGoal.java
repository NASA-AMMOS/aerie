package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.goals.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestRecurrenceGoal {

  @Test
  public void testRecurrence() {
    var actType = new ActivityType("RecGoalActType", null, DurationType.controllable("duration"));
    var planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0),TestUtility.timeFromEpochSeconds(20));
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(Window.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(20, Duration.SECONDS)))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .duration(Duration.of(2, Duration.SECONDS))
                            .ofType(actType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .build();

    Problem problem = new Problem(null, planningHorizon, null, null);

    problem.setGoals(List.of(goal));

    final var solver = new PrioritySolver(problem);

    var plan = solver.getNextSolution().orElseThrow();
    assert(TestUtility.activityStartingAtTime(plan,Duration.of(1, Duration.SECONDS), actType));
    assert(TestUtility.activityStartingAtTime(plan,Duration.of(6, Duration.SECONDS), actType));
    assert(TestUtility.activityStartingAtTime(plan,Duration.of(11, Duration.SECONDS), actType));
    assert(TestUtility.activityStartingAtTime(plan,Duration.of(16, Duration.SECONDS), actType));

  }

}
