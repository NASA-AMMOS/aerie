package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;

public class RadarBeamCalibrationTest {

  /**
   * span of time over which the scheduler should run
   */
  private final PlanningHorizon horizon = new PlanningHorizon(
      Time.fromString("2025-001T00:00:00.000"),
      Time.fromString("2027-001T00:00:00.000"));

  //private final AltitudeDerivativeState altitudeDerivativeState = new AltitudeDerivativeState(horizon);
  private final AltitudeAboveMoon altitudeAboveMoon = new AltitudeAboveMoon(horizon);
  private final FlybyEnumState flybyEnumState = new FlybyEnumState(horizon);

  private Problem problem;
  private Plan plan;

  @BeforeEach
  public void setUp() {
    problem = new Problem(null, horizon, null, null);
    plan = new PlanInMemory();
  }

  @AfterEach
  public void tearDown() {
    problem = null;
    plan = null;
  }


  /**
   #1: radar beam calibration
   aka: reason_active_beam_characterization (row 141)
   users: ansel rothstein-dowden
   descr: collect radar data in +/-7deg cross-track slew across another moon before europa
   constraints:
   - occurs 1x before first europa flyby
   - altitude above moon (non-Europa) < 10kkm
   - solar arrays fixed at -90deg (ie usual closest approach config)
   - prefer avoid eclipse (due to thermal noise)
   - must be nadir pointed below 400km
   - slew agility constrained below 10kkm threshold
   - consists of:
   - A: radar on
   - B: setup slew from (presumably) nadir to +7deg cross-track
   - C: scan from +7 back to nadir
   - D: arrive at nadir at 400km altitude inbound
   - E: scan from nadir to -7deg at 400km outbound
   - F: cleanup slew from -7deg back to (presumably) nadir
   - G: radar off
   **/


  /**
   * Mock orbit phases possible states
   */
  public enum FlybysEnum {
    FIRSTFLYBY,
    SECONDFLYBY,
    THIRDFLYBY,
    NOFLYBY
  }


  /**
   * Hardcoded state describing some two-value orbit phases
   */
  public static class FlybyEnumState extends MockState<FlybysEnum> {

    public FlybyEnumState(PlanningHorizon horizon) {
      type = SupportedTypes.STRING;
      values = new HashMap<>() {{
        put(
            Window.betweenClosedOpen(horizon.getHor().start, Time.fromString("2025-180T00:00:00.000", horizon)),
            FlybysEnum.NOFLYBY);
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-180T00:00:00.000", horizon),
                Time.fromString("2025-185T00:00:00.000", horizon)),
            FlybysEnum.FIRSTFLYBY);
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-185T00:00:00.000", horizon),
                Time.fromString("2025-230T00:00:00.000", horizon)),
            FlybysEnum.NOFLYBY);
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-230T00:00:00.000", horizon),
                Time.fromString("2025-250T00:00:00.000", horizon)),
            FlybysEnum.SECONDFLYBY);
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-250T00:00:00.000", horizon),
                Time.fromString("2025-300T00:00:00.000", horizon)),
            FlybysEnum.NOFLYBY);
        put(
            Window.betweenClosedOpen(
                Time.fromString("2025-300T00:00:00.000", horizon),
                Time.fromString("2025-310T00:00:00.000", horizon)),
            FlybysEnum.THIRDFLYBY);
        put(
            Window.betweenClosedOpen(Time.fromString("2025-310T00:00:00.000", horizon), horizon.getHor().end),
            FlybysEnum.NOFLYBY);
      }};
    }
  }

  /**
   * Hardcoded state describing some altitude state
   */
  public static class AltitudeAboveMoon extends MockState<Integer> {

    public AltitudeAboveMoon(PlanningHorizon horizon) {
      type = SupportedTypes.LONG;
      values = new HashMap<>() {{
        put(Window.betweenClosedOpen(horizon.getHor().start, Time.fromString("2025-180T00:00:00.000", horizon)), 10);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-180T00:00:00.000", horizon),
            Time.fromString("2025-183T00:00:00.000", horizon)), 20);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-183T00:00:00.000", horizon),
            Time.fromString("2025-185T00:00:00.000", horizon)), 30);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-185T00:00:00.000", horizon),
            Time.fromString("2025-202T00:00:00.000", horizon)), 40);
        put(Window.betweenClosedOpen(
            Time.fromString("2025-202T00:00:00.000", horizon),
            Time.fromString("2025-203T00:00:00.000", horizon)), 50);
        put(Window.betweenClosedOpen(Time.fromString("2025-203T00:00:00.000", horizon), horizon.getHor().end), 60);
      }};
    }

  }


}
