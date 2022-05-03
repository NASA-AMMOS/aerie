package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.simulation.SimResource;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class SimulationFacadeTest {

  MissionModel<?> missionModel;
  Problem problem;
  SimulationFacade facade;
  //concrete named time points used to setup tests and validate expectations
  private static final Instant t0h = TestUtility.timeFromEpochMillis(0);
  private static final Instant t1h = TestUtility.timeFromEpochMillis(1000);
  private static final Instant t1_5h = TestUtility.timeFromEpochMillis(1500);
  private static final Instant t2h = TestUtility.timeFromEpochMillis(2000);
  private static final Instant tEndh = TestUtility.timeFromEpochMillis(5000);

  //hard-coded range of scheduling/simulation operations
  private static final PlanningHorizon horizon = new PlanningHorizon(t0h, tEndh);
  private static final Windows entireHorizon = new Windows(horizon.getHor());

  //concrete named time points used to setup tests and validate expectations
  private static final Duration t0 = horizon.toDur(t0h);
  private static final Duration t1 = horizon.toDur(t1h);
  private static final Duration t1_5 = horizon.toDur(t1_5h);
  private static final Duration t2 = horizon.toDur(t2h);
  private static final Duration tEnd = horizon.toDur(tEndh);

  /** fetch reference to the fruit resource in the mission model **/
  private SimResource getFruitRes() {
    return facade.getResource("/fruit");
  }

  /** fetch reference to the conflicted marker on the flag resource in the mission model **/
  private SimResource getFlagConflictedRes() {
    return facade.getResource("/flag/conflicted");
  }

  /** fetch reference to the flag resource in the mission model **/
  private SimResource getFlagRes() {
    return facade.getResource("/flag");
  }

  /** fetch reference to the plant resource in the mission model **/
  private SimResource getPlantRes() {
    return facade.getResource("/plant");
  }

  @BeforeEach
  public void setUp() {
    missionModel = SimulationUtility.getBananaMissionModel();
    final var schedulerModel = SimulationUtility.getBananaSchedulerModel();
    facade = new SimulationFacade(horizon, missionModel);
    problem = new Problem(missionModel, horizon, facade, schedulerModel);
  }

  @AfterEach
  public void tearDown() {
    missionModel = null;
    problem = null;
    facade = null;
  }

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory();
  }

  @Test
  public void doubleConstraintEvalOnEmptyPlan() {
    final var constraint = new StateConstraintExpression.Builder().above(getFruitRes(), SerializedValue.of(2.9)).build();
    final var plan = makeEmptyPlan();
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void boolConstraintEvalOnEmptyPlan() {
    final var constraint = new StateConstraintExpression.Builder().equal(getFlagConflictedRes(), SerializedValue.of(false)).build();
    final var plan = makeEmptyPlan();
    final var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void stringConstraintEvalOnEmptyPlan() {
    final var constraint = new StateConstraintExpression.Builder().equal(getFlagRes(), SerializedValue.of("A")).build();
    final var plan = makeEmptyPlan();
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void intConstraintEvalOnEmptyPlan() {
    final var constraint = new StateConstraintExpression.Builder().equal(getPlantRes(), SerializedValue.of(200)).build();
    final var plan = makeEmptyPlan();
    final var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  /**
   * constructs a simple test plan with one peel and bite
   *
   * expected activity/resource histories:
   * <pre>
   * time: |t0--------|t1--------|t2-------->
   * acts:            |peel(fS)  |bite(0.1)
   * peel: |4.0-------|3.0------------------>
   * fruit:|4.0-------|3.0-------|2.9------->
   * </pre>
   **/
  private PlanInMemory makeTestPlanP0B1() {
    final var plan = makeEmptyPlan();
    final var actTypeBite = problem.getActivityType("BiteBanana");
    final var actTypePeel = problem.getActivityType("PeelBanana");

    var act1 = new ActivityInstance(actTypePeel, t1);
    act1.setArguments(Map.of("peelDirection", SerializedValue.of("fromStem")));
    plan.add(act1);

    var act2 = new ActivityInstance(actTypeBite, t2);
    act2.setArguments(Map.of("biteSize", SerializedValue.of(0.1)));
    plan.add(act2);

    return plan;
  }

  @Test
  public void associationToExistingSatisfyingActivity(){
    final var plan = makeEmptyPlan();
    final var actTypePeel = problem.getActivityType("PeelBanana");
    final var actTypeBite = problem.getActivityType("BiteBanana");

    var act1 = new ActivityInstance(actTypePeel, t1, t2);
    act1.setArguments(Map.of("peelDirection", SerializedValue.of("fromStem")));
    plan.add(act1);

    final var goal = new CoexistenceGoal.Builder()
        .forEach(ActivityExpression.ofType(actTypePeel))
        .forAllTimeIn(horizon.getHor())
        .thereExistsOne(new ActivityCreationTemplate.Builder().
                           ofType(actTypeBite)
                           .withArgument("biteSize", SerializedValue.of(0.1))
                           .build())
        .startsAt(TimeAnchor.END)
        .build();

    problem.setGoals(List.of(goal));
    problem.setInitialPlan(plan);
    final var solver = new PrioritySolver(problem);
    final var plan1 = solver.getNextSolution();
    assertThat(plan1.isPresent()).isTrue();
    final var actAssociatedInFirstRun = plan1.get().getEvaluation().forGoal(goal).getAssociatedActivities();
    assertThat(actAssociatedInFirstRun.size()).isEqualTo(1);
    problem.setInitialPlan(plan1.get());
    final var solver2 = new PrioritySolver(problem);
    final var plan2 = solver2.getNextSolution();
    assertThat(plan2.isPresent()).isTrue();
    final var actAssociatedInSecondRun = plan2.get().getEvaluation().forGoal(goal).getAssociatedActivities();
    assertThat(actAssociatedInSecondRun.size()).isEqualTo(1);
    assertThat(actAssociatedInFirstRun.iterator().next().equalsInProperties(actAssociatedInSecondRun.iterator().next())).isTrue();
  }

  @Test
  public void getValueAtTimeDoubleOnSimplePlanMidpoint() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    var actual = getFruitRes().getValueAtTime(t1_5);
    assertThat(actual).isEqualTo(SerializedValue.of(3.0));
  }

  @Test
  public void getValueAtTimeDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    var actual = getFruitRes().getValueAtTime(t2);
    assertThat(actual).isEqualTo(SerializedValue.of(2.9));
  }

  @Test
  public void whenValueAboveDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    var actual = getFruitRes().whenValueAbove(SerializedValue.of(2.9), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t0, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBelowDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    var actual = getFruitRes().whenValueBelow(SerializedValue.of(3.0), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t2, tEnd));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBetweenDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    var actual = getFruitRes().whenValueBetween(SerializedValue.of(3.00), SerializedValue.of(3.99), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t1, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueEqualDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    var actual = getFruitRes().whenValueEqual(SerializedValue.of(3.00), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t1, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueNotEqualDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    var actual = getFruitRes().whenValueNotEqual(SerializedValue.of(3.00), entireHorizon);
    var expected = new Windows(List.of(Window.betweenClosedOpen(t0, t1), Window.betweenClosedOpen(t2, tEnd)));
    assertThat(actual).isEqualTo(expected);
  }

}
