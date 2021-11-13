package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TestStateConstraints {


  /**
   * span of time over which the scheduler should run
   */
  private final Range<Time> horizon = new Range<>(
      Time.fromString("2025-001T00:00:00.000"),
      Time.fromString("2027-001T00:00:00.000"));

  private final AltitudeDerivativeState altitudeDerivativeState = new AltitudeDerivativeState(horizon);
  private final AltitudeIntegerState altitudeIntegerState = new AltitudeIntegerState(horizon);
  private final EncounterEnumState encounterEnumState = new EncounterEnumState(horizon);

  private MissionModelWrapper missionModel;
  private Plan plan;

  @BeforeEach
  public void setUp() {
    missionModel = new MissionModelWrapper();
    plan = new PlanInMemory(missionModel);
  }

  @AfterEach
  public void tearDown() throws Exception {
    missionModel = null;
    plan = null;
  }


  @Test
  public void testImpliesBoolean() {
    Map<OrbitPhasesEnum, Boolean> mapping = new HashMap<>();
    mapping.put(OrbitPhasesEnum.ENCOUNTER, true);
    mapping.put(OrbitPhasesEnum.NOTENCOUNTER, false);
    ImpliesFromState<OrbitPhasesEnum, Boolean> lunarSomething = new ImpliesFromState<OrbitPhasesEnum, Boolean>(
        encounterEnumState,
        mapping);

    StateConstraintExpression constraintOnImpliesBool = new StateConstraintExpression.Builder().equal(
        lunarSomething,
        false).build();
    StateConstraintExpression constraintEncounter = new StateConstraintExpression.Builder()
        .equal(
            encounterEnumState,
            OrbitPhasesEnum.NOTENCOUNTER)
        .build();


    TimeWindows windows1 = TimeWindows.of(horizon);
    TimeWindows re1 = constraintOnImpliesBool.findWindows(plan, windows1);

    TimeWindows windows2 = TimeWindows.of(horizon);
    TimeWindows re2 = constraintEncounter.findWindows(plan, windows2);

    assert (re1.equals(re2));

  }


  @Test
  public void testor() {
    StateConstraintExpression approachStateConstraint1 = new StateConstraintExpression.Builder().andBuilder()
                                                                                                .equal(
                                                                                                    encounterEnumState,
                                                                                                    OrbitPhasesEnum.ENCOUNTER)
                                                                                                .between(
                                                                                                    altitudeIntegerState,
                                                                                                    20,
                                                                                                    50)
                                                                                                .build();
    assert (approachStateConstraint1.getClass() == StateConstraintExpressionConjunction.class);

    StateConstraintExpression approachStateConstraint2 = new StateConstraintExpression.Builder()
        .above(altitudeDerivativeState, 0.0)
        .build();

    assert (approachStateConstraint2.getClass() == StateConstraintExpressionDisjunction.class);


    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .orBuilder()
        .satisfied(approachStateConstraint1)
        .satisfied(approachStateConstraint2)
        .build();


    TimeWindows windows1 = TimeWindows.of(horizon);
    TimeWindows re1 = approachStateConstraint.findWindows(plan, windows1);

    TimeWindows windows2 = TimeWindows.of(horizon);
    TimeWindows re2 = approachStateConstraint.findWindows(plan, windows2);

    TimeWindows windows3 = TimeWindows.of(horizon);
    TimeWindows re3 = approachStateConstraint.findWindows(plan, windows3);

    re1.union(re2);

    assert (re1.equals(re3));

  }

  /**
   * In this test, one of the flavor of ConstraintGoal is created
   */
  @Test
  public void testconstraintgoal() {

    final var dur = Duration.ofHours(1.0);
    final var activityTypeImage = new ActivityType("EISImage");

    final var eisImageAct = new ActivityCreationTemplate.Builder()
        .ofType(activityTypeImage)
        .duration(dur)
        .build();

    StateConstraintExpression approachStateConstraint1 = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(encounterEnumState, OrbitPhasesEnum.ENCOUNTER)
        .between(altitudeIntegerState, 20, 50)
        .above(altitudeDerivativeState, 0.0)
        .build();

    TimeWindows.setHorizon(Time.fromString("2020-001T00:00:00.000"),Time.fromString("2040-001T00:00:00.000"));
    CoexistenceGoal cg = new CoexistenceGoal.Builder()
        .named("OrbitStateGoal")
        .forAllTimeIn(horizon)
        .thereExistsOne(eisImageAct)
        .forEach(approachStateConstraint1)
        .owned(ChildCustody.Jointly)
        .startsAt(TimeAnchor.START)
        .withPriority(7.0)
        .build();

    Problem problem = new Problem(missionModel);
    problem.add(cg);
    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    final var plan = solver.getNextSolution().orElseThrow();

    TimeWindows windows1 = TimeWindows.of(horizon);
    TimeWindows re1 = approachStateConstraint1.findWindows(plan, windows1);
    assert (TestUtility.atLeastOneActivityOfTypeInTW(plan, re1, activityTypeImage));
  }

  /**
   * In this test, the goal has constraints but not the activity
   */
  @Test
  public void testproceduralgoalwithconstraints() {
    final var dur = Duration.ofHours(1.0);


    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(encounterEnumState, OrbitPhasesEnum.ENCOUNTER)
        .between(altitudeIntegerState, 20, 50)
        .above(altitudeDerivativeState, 0.0)
        .build();

    final var activityTypeImage = new ActivityType("EISImage");

    ActivityInstance act1 = new ActivityInstance("EISImage1_shouldbescheduled", activityTypeImage,
                                                 Time.fromString("2025-202T00:00:00.000"), dur);

    ActivityInstance act2 = new ActivityInstance("EISImage2_shouldnotbescheduled", activityTypeImage,
                                                 Time.fromString("2025-179T00:00:00.000"), dur);

    //create an "external tool" that insists on a few fixed activities
    final var externalActs = java.util.List.of(
        act1,
        act2
    );
    final Function<Plan, Collection<ActivityInstance>> fixedGenerator
        = (p) -> externalActs;

    final var proceduralGoalWithConstraints = new ProceduralCreationGoal.Builder()
        .named("OrbitStateGoalWithTemplateWithProcedural")
        .forAllTimeIn(horizon)
        .attachStateConstraint(approachStateConstraint)
        .generateWith(fixedGenerator)
        .owned(ChildCustody.Jointly)
        .withPriority(7.0)
        .build();

    Problem problem = new Problem(missionModel);
    problem.add(proceduralGoalWithConstraints);
    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    final var plan = solver.getNextSolution().orElseThrow();


    assert (TestUtility.containsExactlyActivity(plan, act1));
    assert (TestUtility.doesNotContainActivity(plan, act2));
  }

  @Test
  public void testCache() {

    StateConstraintExpression approachStateConstraint1 = new StateConstraintExpression.Builder()
        .orBuilder()
        .above(altitudeDerivativeState, 0.0)
        .build();

    StateConstraintExpression approachStateConstraint2 = new StateConstraintExpression.Builder()
        .orBuilder()
        .equal(encounterEnumState, OrbitPhasesEnum.ENCOUNTER)
        .between(altitudeIntegerState, 20, 50)
        .build();

    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .andBuilder()
        .satisfied(approachStateConstraint1)
        .satisfied(approachStateConstraint2)
        .build();

    Time begin = Time.fromString("2025-160T00:00:00.000");
    Time end = Time.fromString("2025-210T00:00:00.000");
    var windows = approachStateConstraint.findWindows(null, TimeWindows.of(new Range<Time>(begin, end)));
    System.out.println(windows);


  }

  /**
   * In this test, the activity itself has constraints but not the goal
   */
  @Test
  public void testactivityconstraints() {
    final var dur = Duration.ofHours(1.0);

    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(encounterEnumState, OrbitPhasesEnum.ENCOUNTER)
        .between(altitudeIntegerState, 20, 50)
        .above(altitudeDerivativeState, 0.0)
        .build();

    final var activityTypeImageWithConstraint = new ActivityType("EISImageWithConstraints", approachStateConstraint);

    ActivityInstance act1 = new ActivityInstance("EISImage1_shouldbescheduled", activityTypeImageWithConstraint,
                                                 Time.fromString("2025-202T00:00:00.000"), dur);

    ActivityInstance act2 = new ActivityInstance("EISImage2_shouldnotbescheduled", activityTypeImageWithConstraint,
                                                 Time.fromString("2025-179T00:00:00.000"), dur);

    //create an "external tool" that insists on a few fixed activities
    final var externalActs = java.util.List.of(
        act1,
        act2
    );

    final Function<Plan, Collection<ActivityInstance>> fixedGenerator
        = (p) -> externalActs;

    final var proceduralgoalwithoutconstraints = new ProceduralCreationGoal.Builder()
        .named("OrbitStateGoalWithTemplateWithProceduralWithoutConstraint")
        .forAllTimeIn(horizon)
        .generateWith(fixedGenerator)
        .owned(ChildCustody.Jointly)
        .withPriority(7.0)
        .build();

    Problem problem = new Problem(missionModel);
    problem.add(proceduralgoalwithoutconstraints);
    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, problem);
    final var plan = solver.getNextSolution().orElseThrow();
    assert (TestUtility.containsExactlyActivity(plan, act1));
    assert (TestUtility.doesNotContainActivity(plan, act2));
  }

  /**
   * Mock orbit phases possible states
   */
  public enum OrbitPhasesEnum {
    NOTENCOUNTER,
    ENCOUNTER;
  }


  /**
   * Hardcoded state describing some two-value orbit phases
   */
  public class EncounterEnumState extends MockState<OrbitPhasesEnum> {

    public EncounterEnumState(Range<Time> horizon) {
      values = new HashMap<Range<Time>, OrbitPhasesEnum>() {{
        put(
            new Range<Time>(horizon.getMinimum(), Time.fromString("2025-180T00:00:00.000")),
            OrbitPhasesEnum.NOTENCOUNTER);
        put(
            new Range<Time>(Time.fromString("2025-180T00:00:00.000"), Time.fromString("2025-185T00:00:00.000")),
            OrbitPhasesEnum.ENCOUNTER);
        put(
            new Range<Time>(Time.fromString("2025-185T00:00:00.000"), Time.fromString("2025-200T00:00:00.000")),
            OrbitPhasesEnum.NOTENCOUNTER);
        put(
            new Range<Time>(Time.fromString("2025-200T00:00:00.000"), Time.fromString("2025-205T00:00:00.000")),
            OrbitPhasesEnum.ENCOUNTER);
        put(
            new Range<Time>(Time.fromString("2025-205T00:00:00.000"), horizon.getMaximum()),
            OrbitPhasesEnum.NOTENCOUNTER);
      }};
    }
  }

  /**
   * Hardcoded state describing some altitude state
   */
  public class AltitudeIntegerState extends MockState<Integer> {

    public AltitudeIntegerState(Range<Time> horizon) {
      values = new HashMap<Range<Time>, Integer>() {{
        put(new Range<Time>(horizon.getMinimum(), Time.fromString("2025-180T00:00:00.000")), 10);
        put(new Range<Time>(Time.fromString("2025-180T00:00:00.000"), Time.fromString("2025-183T00:00:00.000")), 20);
        put(new Range<Time>(Time.fromString("2025-183T00:00:00.000"), Time.fromString("2025-185T00:00:00.000")), 30);
        put(new Range<Time>(Time.fromString("2025-185T00:00:00.000"), Time.fromString("2025-202T00:00:00.000")), 40);
        put(new Range<Time>(Time.fromString("2025-202T00:00:00.000"), Time.fromString("2025-203T00:00:00.000")), 50);
        put(new Range<Time>(Time.fromString("2025-203T00:00:00.000"), horizon.getMaximum()), 60);
      }};
    }

  }

  /**
   * Hardcoded state describing some altitude derivative state
   */
  public class AltitudeDerivativeState extends MockState<Double> {

    public AltitudeDerivativeState(Range<Time> horizon) {
      values = new HashMap<Range<Time>, Double>() {{
        put(new Range<Time>(horizon.getMinimum(), Time.fromString("2025-180T00:00:00.000")), 0.0);
        put(new Range<Time>(Time.fromString("2025-180T00:00:00.000"), Time.fromString("2025-185T00:00:00.000")), 0.0);
        put(new Range<Time>(Time.fromString("2025-185T00:00:00.000"), Time.fromString("2025-200T00:00:00.000")), 0.0);
        put(new Range<Time>(Time.fromString("2025-200T00:00:00.000"), horizon.getMaximum()), 1.0);
      }};
    }

  }


}
