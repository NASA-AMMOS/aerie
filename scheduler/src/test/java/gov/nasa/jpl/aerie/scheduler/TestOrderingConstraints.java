package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class TestOrderingConstraints {

  /**
   * span of time over which the scheduler should run
   */
  private final Range<Time> horizon = new Range<>(
      Time.fromString("2025-001T00:00:00.000"),
      Time.fromString("2027-001T00:00:00.000"));

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
  public void testCardinalityConstraint() {
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance("act1type1", type1, new Time(1), new Duration(4));
    ActivityInstance act2 = new ActivityInstance("act1type2", type2, new Time(3), new Duration(5));

    plan.add(act1);
    CardinalityConstraint cc =
        new CardinalityConstraint.Builder().atMost(0).type(type1).inInterval(new Range<Time>(
            new Time(0),
            new Time(10.))).build();

    //constraint is invalid in interval (0,10)
    ConstraintState cs = cc.isEnforced(plan, TimeWindows.of(new Range<Time>(new Time(0), new Time(10.))));
    assert (cs.isViolation);
    CardinalityConstraint cc2 =
        new CardinalityConstraint.Builder().atMost(0).type(type1).inInterval(new Range<Time>(
            new Time(0),
            new Time(10.))).build();

    //constraint is valid in interval (8,10)
    ConstraintState cs2 = cc2.isEnforced(plan, TimeWindows.of(new Range<Time>(new Time(8), new Time(10.))));
    assert (!cs2.isViolation);
  }


  @Test
  public void testMutex() {
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance("act1type1", type1, new Time(1), new Duration(4));
    ActivityInstance act2 = new ActivityInstance("act1type2", type2, new Time(3), new Duration(5));

    plan.add(act1);
    plan.add(act2);
    BinaryMutexConstraint mc = OrderingConstraint.buildMutexConstraint(type1, type2);

    //constraint is invalid in interval (0,10)
    ConstraintState cs = mc.isEnforced(plan, TimeWindows.of(new Range<Time>(new Time(0), new Time(10.))));
    assert (cs.isViolation);
    assert (cs.violationWindows.equals(TimeWindows.of(new Range<>(new Time(3), new Time(5)))));

    //constraint is valid in interval (8,10)
    cs = mc.isEnforced(plan, TimeWindows.of(new Range<Time>(new Time(8), new Time(10.))));
    assert (!cs.isViolation);
    assert (cs.violationWindows == null);


  }


  @Test
  public void testMutexFindWindows() {
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance("act1type1", type1, new Time(1), new Duration(4));
    ActivityInstance act2 = new ActivityInstance("act1type2", type2, new Time(3), new Duration(5));

    plan.add(act1);
    plan.add(act2);
    BinaryMutexConstraint mc = OrderingConstraint.buildMutexConstraint(type1, type2);


    TimeWindows tw1 = TimeWindows.of(new Range<Time>(new Time(0), new Time(10.)));

    TimeWindows foundWindows = mc.findWindows(
        plan,
        tw1,
        new MissingActivityInstanceConflict(new ActivityExistentialGoal(), act2));

    TimeWindows expectedWindows = new TimeWindows(tw1);
    expectedWindows.substraction(new Range<Time>(new Time(1), new Time(5)));
    assert (foundWindows.equals(expectedWindows));


  }


  @Test
  public void testCardinalityFindWindows() {
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance("act1type1", type1, new Time(1), new Duration(4));

    plan.add(act1);
    CardinalityConstraint cc =
        new CardinalityConstraint.Builder().atMost(1).type(type1).inInterval(new Range<Time>(
            new Time(0),
            new Time(10.))).build();

    TimeWindows tw1 = TimeWindows.of(new Range<Time>(new Time(0), new Time(10.)));

    TimeWindows foundWindows = cc.findWindows(
        plan,
        tw1,
        new MissingActivityInstanceConflict(new ActivityExistentialGoal(), act1));

    TimeWindows expectedWindows = new TimeWindows();
    assert (foundWindows.equals(expectedWindows));


  }


}
