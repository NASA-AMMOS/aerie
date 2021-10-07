package gov.nasa.jpl.aerie.scheduler;


import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

public class TestCardinalityGoal {


  @Test
  public void testone() {
    Range<Time> period = new Range<Time>(new Time(0), new Time(20));
    final var horizon = new Range<>(new Time(0),new Time(25));
    TimeWindows.setHorizon(horizon.getMinimum(),horizon.getMaximum());

    var periodTre = new TimeRangeExpression.Builder()
        .from(TimeWindows.of(period))
        .build();
    ActivityType actType = new ActivityType("CardGoalActType");


    CardinalityGoal goal = new CardinalityGoal.Builder()
        .inPeriod(periodTre)
        .duration(new Range<Duration>(Duration.ofSeconds(12), Duration.ofSeconds(15)))
        .occurences(new Range<Integer>(3, 10))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actType)
                            .duration(Duration.ofSeconds(2))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(period)
        .owned(ChildCustody.Jointly)
        .withPriority(7.0)

        .build();

    MissionModel missionModel = new MissionModel();
    Problem problem = new Problem(missionModel);

    problem.add(goal);

    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    var plan = new PlanInMemory(problem.getMissionModel());


    var evaluation = new Evaluation();
    plan.addEvaluation(evaluation);
    goal.getConflictsDurAndOccur(plan, Duration.ofSeconds(12), 3);
  }


  @Test
  public void testwindows() {

    Range<Time> period = new Range<Time>(new Time(0), new Time(10));
    Range<Time> period2 = new Range<Time>(new Time(13), new Time(20));
    final var horizon = new Range<>(new Time(0),new Time(25));
    TimeWindows.setHorizon(horizon.getMinimum(),horizon.getMaximum());


    var periodTre = new TimeRangeExpression.Builder()
        .from(TimeWindows.of(List.of(period, period2)))
        .build();
    ActivityType actType = new ActivityType("CardGoalActType");


    CardinalityGoal goal = new CardinalityGoal.Builder()
        .inPeriod(periodTre)
        .duration(new Range<Duration>(Duration.ofSeconds(12), Duration.ofSeconds(15)))
        .occurences(new Range<Integer>(3, 10))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actType)
                            .duration(new Range<Duration>(Duration.ofSeconds(2), Duration.ofSeconds(4)))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(period)
        .owned(ChildCustody.Jointly)
        .withPriority(7.0)

        .build();

    MissionModel missionModel = new MissionModel();
    Problem problem = new Problem(missionModel);

    problem.add(goal);

    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    var plan = new PlanInMemory(problem.getMissionModel());


    var evaluation = new Evaluation();
    plan.addEvaluation(evaluation);
    Collection<Conflict> conflicts = goal.getConflictsDurAndOccur(plan, Duration.ofSeconds(12), 3);
    for (var conflict : conflicts) {
      System.out.println(conflict);
    }

  }

}
