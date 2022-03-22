package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpression;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.goals.RecurrenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class UncontrollableDurationTest {

  PlanningHorizon planningHorizon;
  Problem problem;
  Plan plan;
  AerieController controller;

  @BeforeEach
  void setUp(){
    planningHorizon = new PlanningHorizon(new Time(0), new Time(3000));
    MissionModel<?> aerieLanderMissionModel = SimulationUtility.getFooMissionModel();
    problem = new Problem(aerieLanderMissionModel, planningHorizon, new SimulationFacade(planningHorizon, aerieLanderMissionModel), SimulationUtility.getFooSchedulerModel());
    plan = makeEmptyPlan();
  }

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory();
  }
  @Test
  public void testNonLinear(){

    //duration should be 300 seconds trapezoidal
    final var solarPanelActivityTrapezoidal = new ActivityCreationTemplate.Builder()
        .ofType(problem.getActivityType("SolarPanelNonLinear"))
        .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
        .withArgument("theta_turn", SerializedValue.of(2.))
        .withArgument("alpha_max", SerializedValue.of(0.0001))
        .withArgument("omega_max", SerializedValue.of(0.01))
        .build();

    //turn should be 89.44 secs and triangle shape
    final var solarPanelActivityTriangle = new ActivityCreationTemplate.Builder()
        .ofType(problem.getActivityType("SolarPanelNonLinear"))
        .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
        .withArgument("theta_turn", SerializedValue.of(0.2))
        .withArgument("alpha_max", SerializedValue.of(0.0001))
        .withArgument("omega_max", SerializedValue.of(0.01))
        .build();

    final var recurrenceTrapezoidal = new RecurrenceGoal.Builder()
        .thereExistsOne(solarPanelActivityTriangle)
        .forAllTimeIn(planningHorizon.getHor())
        .repeatingEvery(Duration.of(1000, Duration.SECONDS))
        .named("UncontrollableRecurrenceGoal")
        .build();


    final var coexistenceTriangle = new CoexistenceGoal.Builder()
        .thereExistsOne(solarPanelActivityTrapezoidal)
        .forAllTimeIn(planningHorizon.getHor())
        .forEach(solarPanelActivityTriangle)
        .endsAt(TimeAnchor.START)
        .named("UncontrollableCoexistenceGoal")
        .build();


    problem.setGoals(List.of(recurrenceTrapezoidal, coexistenceTriangle));

    final var solver = new PrioritySolver(problem);
    solver.checkSimBeforeInsertingActInPlan();
    final var plan = solver.getNextSolution().get();
    solver.printEvaluation();
  }

  @Test
  public void testTimeDependent(){

    final var solarPanelActivityTrapezoidal = new ActivityCreationTemplate.Builder()
        .ofType(problem.getActivityType("SolarPanelNonLinearTimeDependent"))
        .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
        .withArgument("command", SerializedValue.of(1.))
        .withArgument("alpha_max", SerializedValue.of(0.0001))
        .withArgument("omega_max", SerializedValue.of(0.01))
        .build();

    final var solarPanelActivityTriangle = new ActivityCreationTemplate.Builder()
        .ofType(problem.getActivityType("SolarPanelNonLinearTimeDependent"))
        .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
        .withArgument("command", SerializedValue.of(0.5))
        .withArgument("alpha_max", SerializedValue.of(0.0001))
        .withArgument("omega_max", SerializedValue.of(0.01))
        .build();

    final var recurrenceTrapezoidal = new RecurrenceGoal.Builder()
        .thereExistsOne(solarPanelActivityTriangle)
        .forAllTimeIn(planningHorizon.getHor())
        .repeatingEvery(Duration.of(1000, Duration.SECONDS))
        .named("UncontrollableRecurrenceGoal")
        .build();


    final var start = TimeExpression.atStart();
    final var coexistenceTriangle = new CoexistenceGoal.Builder()
        .thereExistsOne(solarPanelActivityTrapezoidal)
        .forAllTimeIn(planningHorizon.getHor())
        .forEach(solarPanelActivityTriangle)
        .endsAt(start)
        .named("UncontrollableCoexistenceGoal")
        .build();


    problem.setGoals(List.of(recurrenceTrapezoidal, coexistenceTriangle));

    final var solver = new PrioritySolver(problem);
    solver.checkSimBeforeInsertingActInPlan();
    final var plan = solver.getNextSolution().get();
    solver.printEvaluation();
  }

}
