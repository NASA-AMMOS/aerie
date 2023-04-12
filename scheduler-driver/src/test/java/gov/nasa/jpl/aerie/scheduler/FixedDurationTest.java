package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.SpansFromWindows;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpressionRelativeFixed;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.goals.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.*;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FixedDurationTest {

  PlanningHorizon planningHorizon;
  Problem problem;
  Plan plan;

  @BeforeEach
  void setUp(){
    planningHorizon = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochDays(3));
    MissionModel<?> bananaMissionModel = SimulationUtility.getBananaMissionModel();
    problem = new Problem(bananaMissionModel, planningHorizon, new SimulationFacade(planningHorizon, bananaMissionModel), SimulationUtility.getBananaSchedulerModel());
    plan = makeEmptyPlan();
  }

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory();
  }

  @Test
  public void testFieldAnnotation(){

    final var fixedDurationActivityTemplate = new ActivityCreationTemplate.Builder()
        .ofType(problem.getActivityType("BananaNap"))
        .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
        .build();

    final var start = TimeExpression.atStart();
    final var coexistence = new CoexistenceGoal.Builder()
        .thereExistsOne(fixedDurationActivityTemplate)
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(planningHorizon.getHor(), true)))
        .forEach(new SpansFromWindows(new WindowsWrapperExpression(new Windows(false).set(Interval.between(1, 2, Duration.MINUTE), true))))
        .startsAt(start)
        .named("FixedCoexistenceGoal")
        .aliasForAnchors("its a me")
        .build();


    problem.setGoals(List.of(coexistence));

    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().get();
    solver.printEvaluation();
    assertTrue(TestUtility.containsActivity(plan, planningHorizon.fromStart("PT1M"), planningHorizon.fromStart("PT1H1M"), problem.getActivityType("BananaNap")));
  }


  @Test
  public void testMethodAnnotation(){

    final var fixedDurationActivityTemplate = new ActivityCreationTemplate.Builder()
        .ofType(problem.getActivityType("RipenBanana"))
        .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
        .build();

    final var start = TimeExpression.atStart();
    final var coexistence = new CoexistenceGoal.Builder()
        .thereExistsOne(fixedDurationActivityTemplate)
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(false).set(planningHorizon.getHor(), true)))
        .forEach(new SpansFromWindows(new WindowsWrapperExpression(new Windows(false).set(Interval.between(1, 2, Duration.MINUTE), true))))
        .startsAt(start)
        .named("FixedCoexistenceGoal")
        .aliasForAnchors("its a me")
        .build();


    problem.setGoals(List.of(coexistence));

    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().get();
    solver.printEvaluation();
    assertTrue(TestUtility.containsActivity(plan, planningHorizon.fromStart("PT1M"), planningHorizon.fromStart("P2DT1M"), problem.getActivityType("RipenBanana")));
  }

}
