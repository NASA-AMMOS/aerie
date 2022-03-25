package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestOrderingConstraints {
  private Plan plan;
  private static final PlanningHorizon h = new PlanningHorizon(new Time(0), new Time(15));

  private final static Duration t0 = h.toDur(new Time(0));
  private final static Duration t1 = h.toDur(new Time(1));
  private final static Duration t2 = h.toDur(new Time(2));
  private final static Duration t3 = h.toDur(new Time(3));
  private final static Duration t4 = h.toDur(new Time(4));
  private final static Duration t5 = h.toDur(new Time(5));
  private final static Duration t6 = h.toDur(new Time(6));
  private final static Duration t7 = h.toDur(new Time(7));
  private final static Duration t8 = h.toDur(new Time(8));
  private final static Duration t9 = h.toDur(new Time(9));
  private final static Duration t10 = h.toDur(new Time(10));

  @BeforeEach
  public void setUp() {
    plan = new PlanInMemory();
  }

  @AfterEach
  public void tearDown() {
    plan = null;
  }

  @Test
  public void testCardinalityConstraint() {
    PlanningHorizon h = new PlanningHorizon(Time.fromString("2025-001T00:00:00.000"), Time.fromString("2027-001T00:00:00.000"));
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4, Duration.SECONDS));
    ActivityInstance act2 = new ActivityInstance(type2, t3, Duration.of(5, Duration.SECONDS));

    plan.add(act1);
    CardinalityConstraint cc =
        new CardinalityConstraint.Builder().atMost(0).type(type1).inInterval(Window.between(
            t0,
            t10)).build();

    //constraint is invalid in interval (0,10)
    ConstraintState cs = cc.isEnforced(plan, new Windows(Window.between(t0, h.toDur(new Time(10.)))));
    assert (cs.isViolation);
    CardinalityConstraint cc2 =
        new CardinalityConstraint.Builder().atMost(0).type(type1).inInterval(Window.between(
            t0,
                    t10)).build();

    //constraint is valid in interval (8,10)
    ConstraintState cs2 = cc2.isEnforced(plan, new Windows(Window.between(t8, t10)));
    assert (!cs2.isViolation);
  }


  @Test
  public void testMutex() {
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4, Duration.SECONDS));
    ActivityInstance act2 = new ActivityInstance(type2, t3, Duration.of(5, Duration.SECONDS));

    plan.add(act1);
    plan.add(act2);
    BinaryMutexConstraint mc = OrderingConstraint.buildMutexConstraint(type1, type2);

    //constraint is invalid in interval (0,10)
    ConstraintState cs = mc.isEnforced(plan, new Windows(Window.between(t0, t10)));
    assert (cs.isViolation);
    assert (cs.violationWindows.equals(new Windows(Window.betweenClosedOpen(t3, t5))));

    //constraint is valid in interval (8,10)
    cs = mc.isEnforced(plan, new Windows(Window.between(t8, t10)));
    assert (!cs.isViolation);
    assert (cs.violationWindows == null);


  }

  /**
   * Note: self mutex is not defined in effect
   */
  @Test
  public void testMutexFindWindows() {
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4,Duration.SECONDS));
    ActivityInstance act2 = new ActivityInstance(type2, t3, Duration.of(5, Duration.SECONDS));

    plan.add(act1);
    plan.add(act2);
    BinaryMutexConstraint mc = OrderingConstraint.buildMutexConstraint(type1, type2);
    Windows tw1 = new Windows(Window.between(t0, t10));

    var foundWindows = mc.findWindows(
        plan,
        tw1,
        new MissingActivityInstanceConflict(new ActivityExistentialGoal(), act2));

    Windows expectedWindows = new Windows(tw1);
    expectedWindows.subtract(Window.between(t1, Window.Inclusivity.Inclusive, t5, Window.Inclusivity.Exclusive));
    assert (foundWindows.equals(expectedWindows));


  }

  @Test
  public void testCardinalityFindWindows() {
    ActivityType type1 = new ActivityType("Type1");
    ActivityType type2 = new ActivityType("Type2");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4,Duration.SECONDS));

    plan.add(act1);
    CardinalityConstraint cc =
        new CardinalityConstraint.Builder().atMost(1).type(type1).inInterval(Window.between(
            t0,
            t10)).build();

    Windows tw1 = new Windows(Window.between(t0, t10));

    Windows foundWindows = cc.findWindows(
        plan,
        tw1,
        new MissingActivityInstanceConflict(new ActivityExistentialGoal(), act1));

    Windows expectedWindows = new Windows();
    assert (foundWindows.equals(expectedWindows));
  }
}
