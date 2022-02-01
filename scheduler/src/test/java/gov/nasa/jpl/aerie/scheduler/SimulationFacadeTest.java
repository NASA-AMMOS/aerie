package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimulationFacadeTest {

  MissionModel<?> missionModel;
  MissionModelWrapper wrappedMissionModel;
  SimulationFacade facade;
  //concrete named time points used to setup tests and validate expectations
  private static final Time t0h = Time.fromMilli(0);
  private static final Time t1h = Time.fromMilli(1000);
  private static final Time t1_5h = Time.fromMilli(1500);
  private static final Time t2h = Time.fromMilli(2000);
  private static final Time tEndh = Time.fromMilli(5000);

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
    missionModel = SimulationUtility.getMissionModel();
    wrappedMissionModel = new MissionModelWrapper(missionModel, horizon);
    facade = new SimulationFacade(horizon, missionModel);
  }

  @AfterEach
  public void tearDown() {
    missionModel = null;
    wrappedMissionModel = null;
    facade = null;
  }

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory(wrappedMissionModel);
  }

  @Test
  public void constraintEvalWithoutSimulationThrowsIAE() {
    final var constraint = StateConstraintExpression.buildAboveConstraint(getFruitRes(), SerializedValue.of(2.9));
    final var plan = makeEmptyPlan();
    assertThrows(IllegalArgumentException.class, () -> constraint.findWindows(plan, entireHorizon));
  }

  @Test
  public void doubleConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildAboveConstraint(getFruitRes(), SerializedValue.of(2.9));
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void boolConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getFlagConflictedRes(), SerializedValue.of(false));
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    final var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void stringConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getFlagRes(), SerializedValue.of("A"));
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void intConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getPlantRes(), SerializedValue.of(200));
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
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
    final var actTypeBite = wrappedMissionModel.getActivityType("BiteBanana");
    final var actTypePeel = wrappedMissionModel.getActivityType("PeelBanana");

    var act1 = new ActivityInstance(actTypePeel, t1);
    act1.setParameters(Map.of("peelDirection", SerializedValue.of("fromStem")));
    plan.add(act1);

    var act2 = new ActivityInstance(actTypeBite, t2);
    act2.setParameters(Map.of("biteSize", SerializedValue.of(0.1)));
    plan.add(act2);

    return plan;
  }

  @Test
  public void getValueAtTimeDoubleOnSimplePlanMidpoint() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().getValueAtTime(t1_5);
    assertThat(actual).isEqualTo(SerializedValue.of(3.0));
  }

  @Test
  public void getValueAtTimeDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().getValueAtTime(t2);
    assertThat(actual).isEqualTo(SerializedValue.of(2.9));
  }

  @Test
  public void whenValueAboveDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueAbove(SerializedValue.of(2.9), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t0, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBelowDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueBelow(SerializedValue.of(3.0), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t2, tEnd));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBetweenDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueBetween(SerializedValue.of(3.00), SerializedValue.of(3.99), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t1, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueEqualDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueEqual(SerializedValue.of(3.00), entireHorizon);
    var expected = new Windows(Window.betweenClosedOpen(t1, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueNotEqualDoubleOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueNotEqual(SerializedValue.of(3.00), entireHorizon);
    var expected = new Windows(List.of(Window.betweenClosedOpen(t0, t1), Window.betweenClosedOpen(t2, tEnd)));
    assertThat(actual).isEqualTo(expected);
  }

}
