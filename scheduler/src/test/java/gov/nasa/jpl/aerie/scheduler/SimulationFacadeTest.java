package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.google.common.truth.Truth.assertThat;

public class SimulationFacadeTest {

  Adaptation<?, ?> adaptation;
  MissionModel missionModel;
  SimulationFacade facade;

  //concrete named time points used to setup tests and validate expectations
  private static final Time beginHorizon = Time.fromMilli(0);
  private static final Time endHorizon = Time.fromMilli(5000);
  private static final Time t0 = Time.fromMilli(0);
  private static final Time t1 = Time.fromMilli(1500);

  //hard-coded range of scheduling/simulation operations
  private static final Range<Time> planningHorizon = new Range<>(beginHorizon, endHorizon);
  private static final TimeWindows entireHorizon = TimeWindows.of(planningHorizon);

  //scheduler-side mirrors of test activity types used
  //TODO: should eventually mirror these from the adaptation itself
  private static final ActivityType actTypeBite = new ActivityType("BiteBanana");
  private static final ActivityType actTypePeel = new ActivityType("PeelBanana");

  /** fetch reference to the fruit resource in the mission model **/
  private SimResource<Double> getFruitRes() {
    return facade.getDoubleResource("/fruit");
  }

  /** fetch reference to the conflicted marker on the flag resource in the mission model **/
  private SimResource<Boolean> getFlagConflictedRes() {
    return facade.getBooleanResource("/flag/conflicted");
  }

  /** fetch reference to the flag resource in the mission model **/
  private SimResource<String> getFlagRes() {
    return facade.getStringResource("/flag");
  }

  /** fetch reference to the plant resource in the mission model **/
  private SimResource<Integer> getPlantRes() {
    return facade.getIntResource("/plant");
  }

  @BeforeEach
  public void setUp() {
    adaptation = SimulationUtility.getAdaptation();

    missionModel = new MissionModel(adaptation, planningHorizon);
    missionModel.add(actTypeBite);
    missionModel.add(actTypePeel);

    facade = new SimulationFacade(planningHorizon, adaptation);
  }

  @AfterEach
  public void tearDown() {
    adaptation = null;
    missionModel = null;
    facade = null;
  }

  /** constructs an empty plan with the test model/horizon **/
  private PlanInMemory makeEmptyPlan() {
    return new PlanInMemory(missionModel);
  }

  @Test
  public void constraintEvalWithoutSimulationThrowsIAE() {
    final var constraint = StateConstraintExpression.buildAboveConstraint(getFruitRes(), 2.9);
    final var plan = makeEmptyPlan();
    assertThrows(IllegalArgumentException.class, () -> constraint.findWindows(plan, entireHorizon));
  }

  @Test
  public void doubleConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildAboveConstraint(getFruitRes(), 2.9);
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void boolConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getFlagConflictedRes(), false);
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    final var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void stringConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getFlagRes(), "A");
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  @Test
  public void intConstraintEvalOnEmptyPlan() {
    final var constraint = StateConstraintExpression.buildEqualConstraint(getPlantRes(), 200);
    final var plan = makeEmptyPlan();
    facade.simulatePlan(plan);
    final var actual = constraint.findWindows(plan, entireHorizon);
    assertThat(actual).isEqualTo(entireHorizon);
  }

  /**
   * constructs a simple test plan with one peel and bite:
   * time:-------|t0--------|t1-------->
   * |peel(fS)  |bite(0.1)
   *
   * should result in resource histories:
   * time:-------|t0--------|t1-------->
   * peel:4.0----|3.0------------------>
   * fruit:4.0----|3.0-------|2.9------->
   **/
  private PlanInMemory makeTestPlanP0B1() {
    final var plan = makeEmptyPlan();

    var act1 = new ActivityInstance("PeelBanana1", actTypePeel, t0);
    act1.setParameters(Map.of("peelDirection", "fromStem"));
    plan.add(act1);

    var act2 = new ActivityInstance("BiteBanana1", actTypeBite, t1);
    act2.setParameters(Map.of("biteSize", 0.1));
    plan.add(act2);

    return plan;
  }

  @Test
  public void doubleWhenValueAboveOnSimplePlan() {
    facade.simulatePlan(makeTestPlanP0B1());
    var actual = getFruitRes().whenValueAbove(2.9, entireHorizon);
    var expected = TimeWindows.of(t0, t1);
    assertThat(actual).isEqualTo(expected);
  }


}
