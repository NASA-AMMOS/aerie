package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
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
  private final PlanningHorizon horizon = new PlanningHorizon(
      Time.fromString("2025-001T00:00:00.000"),
      Time.fromString("2027-001T00:00:00.000"));

  private final AltitudeDerivativeState altitudeDerivativeState = new AltitudeDerivativeState(horizon);
  private final AltitudeIntegerState altitudeIntegerState = new AltitudeIntegerState(horizon);
  private final EncounterEnumState encounterEnumState = new EncounterEnumState(horizon);

  private Problem problem;
  private Plan plan;

  @BeforeEach
  public void setUp() {
    problem = new Problem(null, horizon);
    plan = new PlanInMemory();
  }

  @AfterEach
  public void tearDown() {
    problem = null;
    plan = null;
  }


  @Test
  public void testImpliesBoolean() {
    Map<SerializedValue, SerializedValue> mapping = new HashMap<>();
    mapping.put(SerializedValue.of(OrbitPhasesEnum.ENCOUNTER.name()), SerializedValue.of(true));
    mapping.put(SerializedValue.of(OrbitPhasesEnum.NOTENCOUNTER.name()), SerializedValue.of(false));
    ImpliesFromState lunarSomething = new ImpliesFromState(
        encounterEnumState,
        mapping);

    StateConstraintExpression constraintOnImpliesBool = new StateConstraintExpression.Builder().equal(
        lunarSomething,
        SerializedValue.of(false)).build();
    StateConstraintExpression constraintEncounter = new StateConstraintExpression.Builder()
        .equal(encounterEnumState,
               SerializedValue.of(OrbitPhasesEnum.NOTENCOUNTER.name()))
        .build();


    Windows windows1 = new Windows(horizon.getHor());
    Windows re1 = constraintOnImpliesBool.findWindows(plan, windows1);

    Windows windows2 = new Windows( horizon.getHor());
    Windows re2 = constraintEncounter.findWindows(plan, windows2);

    assert (re1.equals(re2));

  }


  @Test
  public void testor() {
    StateConstraintExpression approachStateConstraint1 =
        new StateConstraintExpression.Builder().andBuilder()
                                               .equal(encounterEnumState, SerializedValue.of(OrbitPhasesEnum.ENCOUNTER.name()))
                                               .between(altitudeIntegerState,SerializedValue.of(20),SerializedValue.of(50))
                                               .build();
    assert (approachStateConstraint1.getClass() == StateConstraintExpressionConjunction.class);

    StateConstraintExpression approachStateConstraint2 = new StateConstraintExpression.Builder()
        .above(altitudeDerivativeState, SerializedValue.of(0.0))
        .build();

    assert (approachStateConstraint2.getClass() == StateConstraintExpressionDisjunction.class);


    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .orBuilder()
        .satisfied(approachStateConstraint1)
        .satisfied(approachStateConstraint2)
        .build();


    Windows windows1 = new Windows( horizon.getHor());
    Windows re1 = approachStateConstraint1.findWindows(plan, windows1);

    Windows windows2 = new Windows( horizon.getHor());
    Windows re2 = approachStateConstraint2.findWindows(plan, windows2);

    Windows windows3 = new Windows( horizon.getHor());
    Windows re3 = approachStateConstraint.findWindows(plan, windows3);

    re1.addAll(re2);

    assert (re1.equals(re3));

  }

  /**
   * In this test, one of the flavor of ConstraintGoal is created
   */
  @Test
  public void testconstraintgoal() {

    final var dur = gov.nasa.jpl.aerie.merlin.protocol.types.Duration.of(1, gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOUR);
    final var activityTypeImage = new ActivityType("EISImage");

    final var eisImageAct = new ActivityCreationTemplate.Builder()
        .ofType(activityTypeImage)
        .duration(dur)
        .build();

    StateConstraintExpression approachStateConstraint1 = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(encounterEnumState, SerializedValue.of(OrbitPhasesEnum.ENCOUNTER.name()))
        .between(altitudeIntegerState, SerializedValue.of(20),SerializedValue.of(50))
        .above(altitudeDerivativeState, SerializedValue.of(0.0))
        .build();

    CoexistenceGoal cg = new CoexistenceGoal.Builder()
        .named("OrbitStateGoal")
        .forAllTimeIn(horizon.getHor())
        .thereExistsOne(eisImageAct)
        .forEach(approachStateConstraint1)
        .owned(ChildCustody.Jointly)
        .startsAt(TimeAnchor.START)
        .withPriority(7.0)
        .build();

    this.problem.add(cg);
    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, this.problem);
    final var plan = solver.getNextSolution().orElseThrow();

    Windows windows1 = new Windows( horizon.getHor());
    Windows re1 = approachStateConstraint1.findWindows(plan, windows1);
    assert (TestUtility.atLeastOneActivityOfTypeInTW(plan, re1, activityTypeImage));
  }

  /**
   * In this test, the goal has constraints but not the activity
   */
  @Test
  public void testproceduralgoalwithconstraints() {
    final var dur = gov.nasa.jpl.aerie.merlin.protocol.types.Duration.of(1, Duration.HOUR);


    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(encounterEnumState, SerializedValue.of(OrbitPhasesEnum.ENCOUNTER.name()))
        .between(altitudeIntegerState, SerializedValue.of(20), SerializedValue.of(50))
        .above(altitudeDerivativeState, SerializedValue.of(0.0))
        .build();

    final var activityTypeImage = new ActivityType("EISImage");

    ActivityInstance act1 = new ActivityInstance(activityTypeImage,
                                                 Time.fromString("2025-202T00:00:00.000",horizon), dur);

    ActivityInstance act2 = new ActivityInstance(activityTypeImage,
                                                 Time.fromString("2025-179T00:00:00.000",horizon), dur);

    //create an "external tool" that insists on a few fixed activities
    final var externalActs = java.util.List.of(
        act1,
        act2
    );
    final Function<Plan, Collection<ActivityInstance>> fixedGenerator
        = (p) -> externalActs;

    final var proceduralGoalWithConstraints = new ProceduralCreationGoal.Builder()
        .named("OrbitStateGoalWithTemplateWithProcedural")
        .forAllTimeIn(horizon.getHor())
        .attachStateConstraint(approachStateConstraint)
        .generateWith(fixedGenerator)
        .owned(ChildCustody.Jointly)
        .withPriority(7.0)
        .build();

    this.problem.add(proceduralGoalWithConstraints);
    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, this.problem);
    final var plan = solver.getNextSolution().orElseThrow();


    assert (TestUtility.containsExactlyActivity(plan, act1));
    assert (TestUtility.doesNotContainActivity(plan, act2));
  }

  @Test
  public void testCache() {

    StateConstraintExpression approachStateConstraint1 = new StateConstraintExpression.Builder()
        .orBuilder()
        .above(altitudeDerivativeState, SerializedValue.of(0.0))
        .build();

    StateConstraintExpression approachStateConstraint2 = new StateConstraintExpression.Builder()
        .orBuilder()
        .equal(encounterEnumState, SerializedValue.of(OrbitPhasesEnum.ENCOUNTER.name()))
        .between(altitudeIntegerState, SerializedValue.of(20), SerializedValue.of(50))
        .build();

    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .andBuilder()
        .satisfied(approachStateConstraint1)
        .satisfied(approachStateConstraint2)
        .build();

    Duration begin = Time.fromString("2025-160T00:00:00.000",horizon);
    Duration end = Time.fromString("2025-210T00:00:00.000", horizon);
    var windows = approachStateConstraint.findWindows(null, new Windows( Window.between(begin, end)));
    System.out.println(windows);


  }

  /**
   * In this test, the activity itself has constraints but not the goal
   */
  @Test
  public void testactivityconstraints() {
    final var dur = gov.nasa.jpl.aerie.merlin.protocol.types.Duration.of(1, gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOUR);

    StateConstraintExpression approachStateConstraint = new StateConstraintExpression.Builder()
        .andBuilder()
        .equal(encounterEnumState, SerializedValue.of(OrbitPhasesEnum.ENCOUNTER.name()))
        .between(altitudeIntegerState, SerializedValue.of(20), SerializedValue.of(50))
        .above(altitudeDerivativeState, SerializedValue.of(0.0))
        .build();

    final var activityTypeImageWithConstraint = new ActivityType("EISImageWithConstraints", approachStateConstraint);

    ActivityInstance act1 = new ActivityInstance(activityTypeImageWithConstraint,
                                                 Time.fromString("2025-202T00:00:00.000",horizon), dur);

    ActivityInstance act2 = new ActivityInstance(activityTypeImageWithConstraint,
                                                 Time.fromString("2025-179T00:00:00.000",horizon), dur);

    //create an "external tool" that insists on a few fixed activities
    final var externalActs = java.util.List.of(
        act1,
        act2
    );

    final Function<Plan, Collection<ActivityInstance>> fixedGenerator
        = (p) -> externalActs;

    final var proceduralgoalwithoutconstraints = new ProceduralCreationGoal.Builder()
        .named("OrbitStateGoalWithTemplateWithProceduralWithoutConstraint")
        .forAllTimeIn(horizon.getHor())
        .generateWith(fixedGenerator)
        .owned(ChildCustody.Jointly)
        .withPriority(7.0)
        .build();

    this.problem.add(proceduralgoalwithoutconstraints);
    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, this.problem);
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

  @Test
  public void testOfEachValue(){
    final var dur = gov.nasa.jpl.aerie.merlin.protocol.types.Duration.of(1, gov.nasa.jpl.aerie.merlin.protocol.types.Duration.HOUR);
    final var activityTypeImage = new ActivityType("EISImage");

    final var eisImageAct = new ActivityCreationTemplate.Builder()
        .ofType(activityTypeImage)
        .duration(dur)
        .build();
    TimeRangeExpression tre = new TimeRangeExpression.Builder()
        .ofEachValue(encounterEnumState)
        .from(new StateConstraintExpression.Builder().lessThan(altitudeDerivativeState, SerializedValue.of(1.)).build())
        .build();

    CoexistenceGoal cg = new CoexistenceGoal.Builder()
        .named("OrbitStateGoal")
        .forAllTimeIn(horizon.getHor())
        .thereExistsOne(eisImageAct)
        .forEach(tre)
        .owned(ChildCustody.Jointly)
        .startsAt(TimeAnchor.START)
        .withPriority(7.0)
        .build();

    this.problem.add(cg);
    HuginnConfiguration huginn = new HuginnConfiguration();
    final var solver = new PrioritySolver(huginn, this.problem);
    final var plan = solver.getNextSolution().orElseThrow();
    assert(TestUtility.activityStartingAtTime(plan, horizon.getHor().start, activityTypeImage));
    assert(TestUtility.activityStartingAtTime(plan, Time.fromString("2025-180T00:00:00.000",horizon), activityTypeImage));
    assert(TestUtility.activityStartingAtTime(plan, Time.fromString("2025-185T00:00:00.000",horizon), activityTypeImage));
  }

  /**
   * Hardcoded state describing some two-value orbit phases
   */
  public static class EncounterEnumState extends MockState<String> {

    public EncounterEnumState(PlanningHorizon horizon) {
      type = SupportedTypes.STRING;
      values = new HashMap<>() {{
        put(
            Window.betweenClosedOpen(horizon.getHor().start, Time.fromString("2025-180T00:00:00.000", horizon)),
            OrbitPhasesEnum.NOTENCOUNTER.name());
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-180T00:00:00.000", horizon),
                Time.fromString("2025-185T00:00:00.000", horizon)),
            OrbitPhasesEnum.ENCOUNTER.name());
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-185T00:00:00.000", horizon),
                Time.fromString("2025-200T00:00:00.000", horizon)),
            OrbitPhasesEnum.NOTENCOUNTER.name());
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-200T00:00:00.000", horizon),
                Time.fromString("2025-205T00:00:00.000", horizon)),
            OrbitPhasesEnum.ENCOUNTER.name());
        put(
            Window.betweenClosedOpen(Time.fromString("2025-205T00:00:00.000", horizon), horizon.getHor().end),
            OrbitPhasesEnum.NOTENCOUNTER.name());
      }};
    }
  }

  /**
   * Hardcoded state describing some altitude state
   */
  public static class AltitudeIntegerState extends MockState<Long> {

    public AltitudeIntegerState(PlanningHorizon horizon) {
      type = SupportedTypes.LONG;
      values = new HashMap<>() {{
        put(Window.betweenClosedOpen(horizon.getHor().start, Time.fromString("2025-180T00:00:00.000", horizon)), 10L);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-180T00:00:00.000", horizon),
            Time.fromString("2025-183T00:00:00.000", horizon)), 20L);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-183T00:00:00.000", horizon),
            Time.fromString("2025-185T00:00:00.000", horizon)), 30L);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-185T00:00:00.000", horizon),
            Time.fromString("2025-202T00:00:00.000", horizon)), 40L);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-202T00:00:00.000", horizon),
            Time.fromString("2025-203T00:00:00.000", horizon)), 50L);
        put(Window.betweenClosedOpen(Time.fromString("2025-203T00:00:00.000", horizon), horizon.getHor().end), 60L);
      }};
    }

  }

  /**
   * Hardcoded state describing some altitude derivative state
   */
  public static class AltitudeDerivativeState extends MockState<Double> {

    public AltitudeDerivativeState(PlanningHorizon horizon) {
      type = SupportedTypes.REAL;
      values = new HashMap<>() {{
        put(Window.betweenClosedOpen(horizon.getHor().start, Time.fromString("2025-180T00:00:00.000", horizon)), 0.0);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-180T00:00:00.000", horizon),
            Time.fromString("2025-185T00:00:00.000", horizon)), 0.0);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-185T00:00:00.000", horizon),
            Time.fromString("2025-200T00:00:00.000", horizon)), 0.0);
        put(Window.betweenClosedOpen(Time.fromString("2025-200T00:00:00.000", horizon), horizon.getHor().end), 1.0);
      }};
    }

  }


}
