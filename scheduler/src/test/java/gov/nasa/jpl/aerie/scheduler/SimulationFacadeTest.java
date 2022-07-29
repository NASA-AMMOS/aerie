package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.All;
import gov.nasa.jpl.aerie.constraints.tree.DiscreteResource;
import gov.nasa.jpl.aerie.constraints.tree.Equal;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThan;
import gov.nasa.jpl.aerie.constraints.tree.GreaterThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.LessThan;
import gov.nasa.jpl.aerie.constraints.tree.LessThanOrEqual;
import gov.nasa.jpl.aerie.constraints.tree.NotEqual;
import gov.nasa.jpl.aerie.constraints.tree.RealResource;
import gov.nasa.jpl.aerie.constraints.tree.RealValue;
import gov.nasa.jpl.aerie.constraints.tree.WindowsWrapperExpression;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpressionConstant;
import gov.nasa.jpl.aerie.scheduler.goals.CoexistenceGoal;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateQueryParam;
import gov.nasa.jpl.aerie.scheduler.goals.ChildCustody;
import gov.nasa.jpl.aerie.scheduler.goals.ProceduralCreationGoal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.Problem;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import gov.nasa.jpl.aerie.scheduler.solver.PrioritySolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  private static final Window entireHorizon = horizon.getHor();

  //concrete named time points used to setup tests and validate expectations
  private static final Duration t0 = horizon.toDur(t0h);
  private static final Duration t1 = horizon.toDur(t1h);
  private static final Duration t1_5 = horizon.toDur(t1_5h);
  private static final Duration t2 = horizon.toDur(t2h);
  private static final Duration tEnd = horizon.toDur(tEndh);

  /** fetch reference to the fruit resource in the mission model **/
  private RealResource getFruitRes() {
    return new RealResource("/fruit");
  }

  /** fetch reference to the conflicted marker on the flag resource in the mission model **/
  private DiscreteResource getFlagConflictedRes() {
    return new DiscreteResource("/flag/conflicted");
  }

  /** fetch reference to the flag resource in the mission model **/
  private DiscreteResource getFlagRes() {
    return new DiscreteResource("/flag");
  }

  /** fetch reference to the plant resource in the mission model **/
  private DiscreteResource getPlantRes() {
    return new DiscreteResource("/plant");
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
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(horizon.getHor())))
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
    facade.computeSimulationResultsUntil(tEnd);
    final var stateQuery = new StateQueryParam(getFruitRes().name(), new TimeExpressionConstant(t1_5));
    final var actual = stateQuery.getValue(facade.getLatestConstraintSimulationResults(), null, horizon.getHor());
    assertThat(actual).isEqualTo(SerializedValue.of(3.0));
  }

  @Test
  public void getValueAtTimeDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    facade.computeSimulationResultsUntil(tEnd);
    final var stateQuery = new StateQueryParam(getFruitRes().name(), new TimeExpressionConstant(t2));
    final var actual = stateQuery.getValue(facade.getLatestConstraintSimulationResults(), null, horizon.getHor());
    assertThat(actual).isEqualTo(SerializedValue.of(2.9));
  }

  @Test
  public void whenValueAboveDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    facade.computeSimulationResultsUntil(tEnd);
    var actual = new GreaterThan(getFruitRes(), new RealValue(2.9)).evaluate(facade.getLatestConstraintSimulationResults());
    var expected = new Windows(Window.betweenClosedOpen(t0, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBelowDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    facade.computeSimulationResultsUntil(tEnd);
    var actual = new LessThan(getFruitRes(), new RealValue(3.0)).evaluate(facade.getLatestConstraintSimulationResults());
    var expected = new Windows(Window.between(t2, tEnd));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueBetweenDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    facade.computeSimulationResultsUntil(tEnd);
    var actual = new All(new GreaterThanOrEqual(getFruitRes(), new RealValue(3.0)), new LessThanOrEqual(getFruitRes(), new RealValue(3.99))).evaluate(facade.getLatestConstraintSimulationResults());
    var expected = new Windows(Window.betweenClosedOpen(t1, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueEqualDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    facade.computeSimulationResultsUntil(tEnd);
    var actual = new Equal<>(getFruitRes(), new RealValue(3.0)).evaluate(facade.getLatestConstraintSimulationResults());
    var expected = new Windows(Window.betweenClosedOpen(t1, t2));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void whenValueNotEqualDoubleOnSimplePlan() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());
    facade.computeSimulationResultsUntil(tEnd);
    var actual = new NotEqual<>(getFruitRes(), new RealValue(3.0)).evaluate(facade.getLatestConstraintSimulationResults());
    var expected = new Windows(List.of(Window.betweenClosedOpen(t0, t1), Window.between(t2, tEnd)));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testCoexistenceGoalWithResourceConstraint() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());

    /**
    * reminder for PB1
     * <pre>
     * time: |t0--------|t1--------|t2-------->
     * acts:            |peel(fS)  |bite(0.1)
     * peel: |4.0-------|3.0------------------>
     * fruit:|4.0-------|3.0-------|2.9------->
     * </pre>
     **/
    final var constraint = new All(
        new LessThanOrEqual(new RealResource("/peel"), new RealValue(3.0)),
        new LessThanOrEqual(new RealResource("/fruit"), new RealValue(2.9))
    );

    final var actTypePeel = problem.getActivityType("PeelBanana");

    CoexistenceGoal cg = new CoexistenceGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(horizon.getHor())))
        .thereExistsOne(new ActivityCreationTemplate.Builder()
                            .ofType(actTypePeel)
                            .withArgument("peelDirection", SerializedValue.of("fromStem"))
                            .build())
        .forEach(constraint)
        .owned(ChildCustody.Jointly)
        .startsAt(TimeAnchor.START)
        .build();

    problem.setGoals(List.of(cg));
    final var solver = new PrioritySolver(this.problem);
    final var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.containsActivity(plan, t2, t2, actTypePeel));
  }


  @Test
  public void testProceduralGoalWithResourceConstraint() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());

    final var constraint = new All(
        new LessThanOrEqual(new RealResource("/peel"), new RealValue(3.0)),
        new LessThanOrEqual(new RealResource("/fruit"), new RealValue(2.9))
    );

    final var actTypePeel = problem.getActivityType("PeelBanana");

    ActivityInstance act1 = new ActivityInstance(actTypePeel,
                                                 t0, Duration.ZERO);

    ActivityInstance act2 = new ActivityInstance(actTypePeel,
                                                 t2, Duration.ZERO);

    //create an "external tool" that insists on a few fixed activities
    final var externalActs = java.util.List.of(
        act1,
        act2
    );
    final Function<Plan, Collection<ActivityInstance>> fixedGenerator
        = (p) -> externalActs;

    final var proceduralGoalWithConstraints = new ProceduralCreationGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(horizon.getHor())))
        .attachStateConstraint(constraint)
        .generateWith(fixedGenerator)
        .owned(ChildCustody.Jointly)
        .build();

    problem.setGoals(List.of(proceduralGoalWithConstraints));
    final var solver = new PrioritySolver(this.problem);
    final var plan = solver.getNextSolution().orElseThrow();

    assertTrue(TestUtility.containsExactlyActivity(plan, act2));
    assertTrue(TestUtility.doesNotContainActivity(plan, act1));
  }

  @Test
  public void testActivityTypeWithResourceConstraint() throws SimulationFacade.SimulationException {
    facade.simulateActivities(makeTestPlanP0B1().getActivities());

    final var constraint = new All(
        new LessThanOrEqual(new RealResource("/peel"), new RealValue(3.0)),
        new LessThanOrEqual(new RealResource("/fruit"), new RealValue(2.9))
    );

    final var actTypePeel = problem.getActivityType("PeelBanana");
    actTypePeel.setResourceConstraint(constraint);

    ActivityInstance act1 = new ActivityInstance(actTypePeel,
                                                 t0, Duration.ZERO);

    ActivityInstance act2 = new ActivityInstance(actTypePeel,
                                                 t2, Duration.ZERO);

    //create an "external tool" that insists on a few fixed activities
    final var externalActs = java.util.List.of(
        act1,
        act2
    );

    final Function<Plan, Collection<ActivityInstance>> fixedGenerator
        = (p) -> externalActs;

    final var proceduralgoalwithoutconstraints = new ProceduralCreationGoal.Builder()
        .forAllTimeIn(new WindowsWrapperExpression(new Windows(horizon.getHor())))
        .generateWith(fixedGenerator)
        .owned(ChildCustody.Jointly)
        .build();

    problem.setGoals(List.of(proceduralgoalwithoutconstraints));
    final var solver = new PrioritySolver(problem);
    final var plan = solver.getNextSolution().orElseThrow();
    assertTrue(TestUtility.containsExactlyActivity(plan, act2));
    assertTrue(TestUtility.doesNotContainActivity(plan, act1));
  }
}
