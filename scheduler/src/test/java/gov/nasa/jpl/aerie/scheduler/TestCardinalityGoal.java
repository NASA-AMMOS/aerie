package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestCardinalityGoal {


  @Test
  public void testone() {
    Window period = Window.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(20, Duration.SECONDS));

    var periodTre = new TimeRangeExpression.Builder()
        .from(new Windows(period))
        .build();
    ActivityType actType = new ActivityType("CardGoalActType");


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
        .withPriority(7.0)

        .build();

    Problem problem = new Problem(null, null);

    problem.add(goal);

    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    var plan = new PlanInMemory();


    var evaluation = new Evaluation();
    plan.addEvaluation(evaluation);
    var conflicts = goal.controllableGetConflictsDurAndOccur(plan, Duration.of(12, Duration.SECONDS), 3);
    assert(conflicts.size()==6);
  }


  @Test
  public void testwindows() {

    Window period = Window.betweenClosedOpen(Duration.of(0, Duration.SECONDS), Duration.of(10, Duration.SECONDS));
    Window period2 = Window.betweenClosedOpen(Duration.of(13, Duration.SECONDS), Duration.of(20, Duration.SECONDS));
    final var horizon = new Range<>(Duration.of(0, Duration.SECONDS),Duration.of(25, Duration.SECONDS));


    var periodTre = new TimeRangeExpression.Builder()
        .from(new Windows(List.of(period, period2)))
        .build();
    ActivityType actType = new ActivityType("CardGoalActType");


    CardinalityGoal goal = new CardinalityGoal.Builder()
        .inPeriod(periodTre)
        .duration(Window.between(Duration.of(12, Duration.SECONDS), Duration.of(15, Duration.SECONDS)))
        .occurences(new Range<>(3, 10))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actType)
                            .duration(Window.between(Duration.of(2, Duration.SECONDS), Duration.of(4, Duration.SECONDS)))
                            .build())
        .named("TestCardGoal")
        .forAllTimeIn(period)
        .owned(ChildCustody.Jointly)
        .withPriority(7.0)

        .build();

    Problem problem = new Problem(null, null);

    problem.add(goal);

    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    var plan = new PlanInMemory();


    var evaluation = new Evaluation();
    plan.addEvaluation(evaluation);
    var conflicts = goal.controllableGetConflictsDurAndOccur(plan, Duration.of(12, Duration.SECONDS), 3);
    assert(conflicts.size()==5);
  }

}
