package gov.nasa.jpl.aerie.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nasa.jpl.aerie.constraints.time.Windows;
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
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UncontrollableDurationTest {

  PlanningHorizon planningHorizon;
  Problem problem;
  Plan plan;

  @BeforeEach
  void setUp() {
    planningHorizon =
        new PlanningHorizon(
            TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochSeconds(3000));
    MissionModel<?> aerieLanderMissionModel = SimulationUtility.getFooMissionModel();
    problem =
        new Problem(
            aerieLanderMissionModel,
            planningHorizon,
            new SimulationFacade(planningHorizon, aerieLanderMissionModel),
            SimulationUtility.getFooSchedulerModel());
    plan = makeEmptyPlan();
  }

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory();
  }

  @Test
  public void testNonLinear() {

    // duration should be 300 seconds trapezoidal
    final var solarPanelActivityTrapezoidal =
        new ActivityCreationTemplate.Builder()
            .ofType(problem.getActivityType("SolarPanelNonLinear"))
            .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
            .withArgument("theta_turn", SerializedValue.of(2.))
            .withArgument("alpha_max", SerializedValue.of(0.0001))
            .withArgument("omega_max", SerializedValue.of(0.01))
            .build();

    // turn should be 89.44 secs and triangle shape
    final var solarPanelActivityTriangle =
        new ActivityCreationTemplate.Builder()
            .ofType(problem.getActivityType("SolarPanelNonLinear"))
            .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
            .withArgument("theta_turn", SerializedValue.of(0.2))
            .withArgument("alpha_max", SerializedValue.of(0.0001))
            .withArgument("omega_max", SerializedValue.of(0.01))
            .build();

    final var recurrenceTrapezoidal =
        new RecurrenceGoal.Builder()
            .thereExistsOne(solarPanelActivityTriangle)
            .forAllTimeIn(
                new WindowsWrapperExpression(
                    new Windows(false).set(planningHorizon.getHor(), true)))
            .repeatingEvery(Duration.of(1000, Duration.SECONDS))
            .named("UncontrollableRecurrenceGoal")
            .build();

    final var coexistenceTriangle =
        new CoexistenceGoal.Builder()
            .thereExistsOne(solarPanelActivityTrapezoidal)
            .forAllTimeIn(
                new WindowsWrapperExpression(
                    new Windows(false).set(planningHorizon.getHor(), true)))
            .forEach(solarPanelActivityTriangle)
            .endsAt(TimeAnchor.START)
            .named("UncontrollableCoexistenceGoal")
            .aliasForAnchors("Bond. James Bond")
            .build();

    problem.setGoals(List.of(recurrenceTrapezoidal, coexistenceTriangle));

    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().get();
    solver.printEvaluation();
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT11M40S"),
            planningHorizon.fromStart("PT16M40S"),
            problem.getActivityType("SolarPanelNonLinear")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT28M20S"),
            planningHorizon.fromStart("PT33M20S"),
            problem.getActivityType("SolarPanelNonLinear")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT0S"),
            planningHorizon.fromStart("PT1M29S"),
            problem.getActivityType("SolarPanelNonLinear")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT16M40S"),
            planningHorizon.fromStart("PT18M9S"),
            problem.getActivityType("SolarPanelNonLinear")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT33M20S"),
            planningHorizon.fromStart("PT34M49S"),
            problem.getActivityType("SolarPanelNonLinear")));
  }

  @Test
  public void testTimeDependent() {

    final var solarPanelActivityTrapezoidal =
        new ActivityCreationTemplate.Builder()
            .ofType(problem.getActivityType("SolarPanelNonLinearTimeDependent"))
            .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
            .withArgument("command", SerializedValue.of(1.))
            .withArgument("alpha_max", SerializedValue.of(0.0001))
            .withArgument("omega_max", SerializedValue.of(0.01))
            .build();

    final var solarPanelActivityTriangle =
        new ActivityCreationTemplate.Builder()
            .ofType(problem.getActivityType("SolarPanelNonLinearTimeDependent"))
            .withTimingPrecision(Duration.of(500, Duration.MILLISECOND))
            .withArgument("command", SerializedValue.of(0.5))
            .withArgument("alpha_max", SerializedValue.of(0.0001))
            .withArgument("omega_max", SerializedValue.of(0.01))
            .build();

    final var recurrenceTrapezoidal =
        new RecurrenceGoal.Builder()
            .thereExistsOne(solarPanelActivityTriangle)
            .forAllTimeIn(
                new WindowsWrapperExpression(
                    new Windows(false).set(planningHorizon.getHor(), true)))
            .repeatingEvery(Duration.of(1000, Duration.SECONDS))
            .named("UncontrollableRecurrenceGoal")
            .build();

    final var start = TimeExpression.atStart();
    final var coexistenceTriangle =
        new CoexistenceGoal.Builder()
            .thereExistsOne(solarPanelActivityTrapezoidal)
            .forAllTimeIn(
                new WindowsWrapperExpression(
                    new Windows(false).set(planningHorizon.getHor(), true)))
            .forEach(solarPanelActivityTriangle)
            .endsAt(start)
            .named("UncontrollableCoexistenceGoal")
            .aliasForAnchors("its a me")
            .build();

    problem.setGoals(List.of(recurrenceTrapezoidal, coexistenceTriangle));

    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().get();
    solver.printEvaluation();
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT0S"),
            planningHorizon.fromStart("PT0S"),
            problem.getActivityType("SolarPanelNonLinearTimeDependent")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT11M57S"),
            planningHorizon.fromStart("PT16M40S"),
            problem.getActivityType("SolarPanelNonLinearTimeDependent")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT28M34S"),
            planningHorizon.fromStart("PT33M20S"),
            problem.getActivityType("SolarPanelNonLinearTimeDependent")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT33M20S"),
            planningHorizon.fromStart("PT36M47S"),
            problem.getActivityType("SolarPanelNonLinearTimeDependent")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT0S"),
            planningHorizon.fromStart("PT2M21S"),
            problem.getActivityType("SolarPanelNonLinearTimeDependent")));
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT16M40S"),
            planningHorizon.fromStart("PT17M18S"),
            problem.getActivityType("SolarPanelNonLinearTimeDependent")));
  }

  @Test
  public void testBug() {
    final var controllableDurationActivity =
        SchedulingActivityDirective.of(
            problem.getActivityType("ControllableDurationActivity"),
            Duration.of(1, Duration.MICROSECONDS),
            Duration.of(3, Duration.MICROSECONDS),
            null,
            true);

    final var zeroDurationUncontrollableActivity =
        new ActivityCreationTemplate.Builder()
            .ofType(problem.getActivityType("ZeroDurationUncontrollableActivity"))
            .withTimingPrecision(Duration.of(1, Duration.MICROSECONDS))
            .build();

    // this time expression produces an interval [TimeAnchor.END, TimeAnchor.END + 2 Ms]
    final var intervalStartTimeExpression = new TimeExpressionRelativeFixed(TimeAnchor.END, false);
    intervalStartTimeExpression.addOperation(
        TimeUtility.Operator.PLUS, Duration.of(2, Duration.MICROSECONDS));

    final var coexistenceControllable =
        new CoexistenceGoal.Builder()
            .thereExistsOne(zeroDurationUncontrollableActivity)
            .forAllTimeIn(
                new WindowsWrapperExpression(
                    new Windows(false).set(planningHorizon.getHor(), true)))
            .forEach(
                ActivityExpression.ofType(problem.getActivityType("ControllableDurationActivity")))
            .startsAt(intervalStartTimeExpression)
            .aliasForAnchors("its a me")
            .build();

    problem.setGoals(List.of(coexistenceControllable));
    final var initialPlan = new PlanInMemory();
    initialPlan.add(controllableDurationActivity);
    problem.setInitialPlan(initialPlan);

    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().get();
    solver.printEvaluation();
    assertTrue(
        TestUtility.containsActivity(
            plan,
            planningHorizon.fromStart("PT0.000004S"),
            planningHorizon.fromStart("PT0.000004S"),
            problem.getActivityType("ZeroDurationUncontrollableActivity")));
  }
}
