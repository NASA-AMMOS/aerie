package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityInstanceConflict;
import gov.nasa.jpl.aerie.scheduler.constraints.scheduling.BinaryMutexConstraint;
import gov.nasa.jpl.aerie.scheduler.constraints.scheduling.CardinalityConstraint;
import gov.nasa.jpl.aerie.scheduler.constraints.scheduling.ConstraintState;
import gov.nasa.jpl.aerie.scheduler.constraints.scheduling.GlobalConstraints;
import gov.nasa.jpl.aerie.scheduler.goals.ActivityExistentialGoal;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanInMemory;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOrderingConstraints {

  /**
   * span of time over which the scheduler should run
   */
  private final Range<Instant> horizon = new Range<>(
      TimeUtility.fromDOY("2025-001T00:00:00.000"),
      TimeUtility.fromDOY("2027-001T00:00:00.000"));

  private Plan plan;
  private static final PlanningHorizon h = new PlanningHorizon(TestUtility.timeFromEpochSeconds(0), TestUtility.timeFromEpochSeconds(15));

  private final static Duration t0 = h.toDur(TestUtility.timeFromEpochSeconds(0));
  private final static Duration t1 = h.toDur(TestUtility.timeFromEpochSeconds(1));
  private final static Duration t2 = h.toDur(TestUtility.timeFromEpochSeconds(2));
  private final static Duration t3 = h.toDur(TestUtility.timeFromEpochSeconds(3));
  private final static Duration t4 = h.toDur(TestUtility.timeFromEpochSeconds(4));
  private final static Duration t5 = h.toDur(TestUtility.timeFromEpochSeconds(5));
  private final static Duration t6 = h.toDur(TestUtility.timeFromEpochSeconds(6));
  private final static Duration t7 = h.toDur(TestUtility.timeFromEpochSeconds(7));
  private final static Duration t8 = h.toDur(TestUtility.timeFromEpochSeconds(8));
  private final static Duration t9 = h.toDur(TestUtility.timeFromEpochSeconds(9));
  private final static Duration t10 = h.toDur(TestUtility.timeFromEpochSeconds(10));

  @BeforeEach
  public void setUp() {
    plan = new PlanInMemory();
  }

  @AfterEach
  public void tearDown() {
    plan = null;
  }

  @Test
  public void testCardinalityConstraint() throws SimulationFacade.SimulationException {
    ActivityType type1 = new ActivityType("ControllableDurationActivity");
    ActivityType type2 = new ActivityType("OtherControllableDurationActivity");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4, Duration.SECONDS), Map.of("duration", SerializedValue.of(Duration.of(4, Duration.SECONDS).in(Duration.MICROSECONDS))));
    ActivityInstance act2 = new ActivityInstance(type2, t3, Duration.of(5, Duration.SECONDS), Map.of("duration", SerializedValue.of(Duration.of(5, Duration.SECONDS).in(Duration.MICROSECONDS))));

    plan.add(act1);
    CardinalityConstraint cc =
        new CardinalityConstraint.Builder().atMost(0).type(type1).inInterval(Interval.between(
            t0,
            t10)).build();

    final var simulationFacade = new SimulationFacade(h, SimulationUtility.getFooMissionModel());
    simulationFacade.simulateActivities(plan.getActivities());

    //constraint is invalid in interval (0,10)
    ConstraintState cs = cc.isEnforced(plan, new Windows(false).set(Interval.between(t0, h.toDur(TestUtility.timeFromEpochSeconds(10))), true), simulationFacade.getLatestConstraintSimulationResults());
    assertTrue(cs.isViolation);
    CardinalityConstraint cc2 =
        new CardinalityConstraint.Builder().atMost(0).type(type1).inInterval(Interval.between(
            t0,
                    t10)).build();

    //constraint is valid in interval (8,10)
    ConstraintState cs2 = cc2.isEnforced(plan, new Windows(false).set(Interval.between(t8, t10), true), simulationFacade.getLatestConstraintSimulationResults());
    assertFalse(cs2.isViolation);
  }


  @Test
  public void testMutex() throws SimulationFacade.SimulationException {
    ActivityType type1 = new ActivityType("ControllableDurationActivity");
    ActivityType type2 = new ActivityType("OtherControllableDurationActivity");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4, Duration.SECONDS), Map.of("duration", SerializedValue.of(Duration.of(4, Duration.SECONDS).in(Duration.MICROSECONDS))));
    ActivityInstance act2 = new ActivityInstance(type2, t3, Duration.of(5, Duration.SECONDS), Map.of("duration", SerializedValue.of(Duration.of(5, Duration.SECONDS).in(Duration.MICROSECONDS))));

    plan.add(act1);
    plan.add(act2);
    BinaryMutexConstraint mc = GlobalConstraints.buildBinaryMutexConstraint(type1, type2);

    final var simulationFacade = new SimulationFacade(h, SimulationUtility.getFooMissionModel());
    simulationFacade.simulateActivities(plan.getActivities());

    //constraint is invalid in interval (0,10)
    ConstraintState cs = mc.isEnforced(plan, new Windows(false).set(Interval.between(t0, t10), true), simulationFacade.getLatestConstraintSimulationResults());
    assertTrue(cs.isViolation);
    assertEquals(cs.violationWindows, new Windows(false).set(Interval.betweenClosedOpen(t3, t5), true));

    //constraint is valid in interval (8,10)
    cs = mc.isEnforced(plan, new Windows(false).set(Interval.between(t8, t10), true), simulationFacade.getLatestConstraintSimulationResults());
    assertFalse(cs.isViolation);
    assertNull(cs.violationWindows);


  }

  /**
   * Note: self mutex is not defined in effect
   */
  @Test
  public void testMutexFindWindows() throws SimulationFacade.SimulationException {
    ActivityType type1 = new ActivityType("ControllableDurationActivity");
    ActivityType type2 = new ActivityType("OtherControllableDurationActivity");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4, Duration.SECONDS), Map.of("duration", SerializedValue.of(Duration.of(4, Duration.SECONDS).in(Duration.MICROSECONDS))));
    ActivityInstance act2 = new ActivityInstance(type2, t3, Duration.of(5, Duration.SECONDS), Map.of("duration", SerializedValue.of(Duration.of(5, Duration.SECONDS).in(Duration.MICROSECONDS))));

    plan.add(act1);
    plan.add(act2);
    BinaryMutexConstraint mc = GlobalConstraints.buildBinaryMutexConstraint(type1, type2);
    Windows tw1 = new Windows(false).set(Interval.between(t0, t10), true);

    final var simulationFacade = new SimulationFacade(h, SimulationUtility.getFooMissionModel());
    simulationFacade.simulateActivities(plan.getActivities());

    var foundWindows = mc.findWindows(
        plan,
        tw1,
        new MissingActivityInstanceConflict(new ActivityExistentialGoal(), act2), null);

    Windows expectedWindows = tw1.and(new Windows(true).set(Interval.between(t1, Interval.Inclusivity.Inclusive, t5, Interval.Inclusivity.Exclusive), false));
    assertEquals(expectedWindows, foundWindows);
  }

  @Test
  public void testCardinalityFindWindows() throws SimulationFacade.SimulationException {
    ActivityType type1 = new ActivityType("ControllableDurationActivity");

    ActivityInstance act1 = new ActivityInstance(type1, t1, Duration.of(4, Duration.SECONDS), Map.of("duration", SerializedValue.of(Duration.of(4, Duration.SECONDS).in(Duration.MICROSECONDS))));

    plan.add(act1);

    final var simulationFacade = new SimulationFacade(h, SimulationUtility.getFooMissionModel());
    simulationFacade.simulateActivities(plan.getActivities());

    CardinalityConstraint cc =
        new CardinalityConstraint.Builder().atMost(1).type(type1).inInterval(Interval.between(
            t0,
            t10)).build();

    Windows tw1 = new Windows(false).set(Interval.between(t0, t10), true);

    Windows foundWindows = cc.findWindows(
        plan,
        tw1,
        new MissingActivityInstanceConflict(new ActivityExistentialGoal(), act1),
        simulationFacade.getLatestConstraintSimulationResults());

    Windows expectedWindows = new Windows(Interval.FOREVER, false);
    assertEquals(foundWindows, expectedWindows);


  }
}
