package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;
public class TestRecurrenceGoal {

  @Test
  public void testRecurrence() {
    var actType = new ActivityType("RecGoalActType");
    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(Window.betweenClosedOpen(Duration.of(1, Duration.SECONDS), Duration.of(20, Duration.SECONDS)))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .duration(Duration.of(2, Duration.SECONDS))
                            .ofType(actType)
                            .build())
        .repeatingEvery(Duration.of(5, Duration.SECONDS))
        .build();

    MissionModelWrapper missionModel = new MissionModelWrapper();
    Problem problem = new Problem(missionModel);

    problem.add(goal);

    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);

    var plan = solver.getNextSolution().orElseThrow();
    assert(TestUtility.activityStartingAtTime(plan,Duration.of(6, Duration.SECONDS), actType));
    assert(TestUtility.activityStartingAtTime(plan,Duration.of(11, Duration.SECONDS), actType));
    assert(TestUtility.activityStartingAtTime(plan,Duration.of(16, Duration.SECONDS), actType));

  }

}
