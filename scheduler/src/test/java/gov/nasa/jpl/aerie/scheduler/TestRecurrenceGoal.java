package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestRecurrenceGoal {

  @Test
  public void testRecurrence() {

    RecurrenceGoal goal = new RecurrenceGoal.Builder()
        .named("Test recurrence goal")
        .forAllTimeIn(new Range<Time>(new Time(1), new Time(20)))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .duration(new Duration(2))
                            .ofType(new ActivityType("RecGoalActType"))
                            .build())
        .repeatingEvery(new Duration(5))
        .build();

    MissionModel missionModel = new MissionModel();
    Problem problem = new Problem(missionModel);

    problem.add(goal);

    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    //var plan = new PlanInMemory( problem.getMissionModel() );

    var plan = solver.getNextSolution().orElseThrow();
    TestUtility.printPlan(plan);

  }

}
