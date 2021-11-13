package gov.nasa.jpl.aerie.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;

public class RadarBeamCalibrationTest {

  /**
   * span of time over which the scheduler should run
   */
  private final Range<Time> horizon = new Range<>(
      Time.fromString("2025-001T00:00:00.000"),
      Time.fromString("2027-001T00:00:00.000"));

  //private final AltitudeDerivativeState altitudeDerivativeState = new AltitudeDerivativeState(horizon);
  private final AltitudeAboveMoon altitudeAboveMoon = new AltitudeAboveMoon(horizon);
  private final FlybyEnumState flybyEnumState = new FlybyEnumState(horizon);

  private MissionModelWrapper missionModel;
  private Plan plan;

  @BeforeEach
  public void setUp() throws Exception {
    missionModel = new MissionModelWrapper();
    plan = new PlanInMemory(missionModel);
  }

  @AfterEach
  public void tearDown() throws Exception {
    missionModel = null;
    plan = null;
  }

  //To develop :
  // - atStartState : state to be valid not for whole activity duration but for start
  // -

  public void test() {


    StateConstraintExpression passWindows = new StateConstraintExpression.Builder()
        .lessThan(altitudeAboveMoon, 10000)
        .build();

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
    NOFLYBY;
  }


  /**
   * Hardcoded state describing some two-value orbit phases
   */
  public class FlybyEnumState extends MockState<FlybysEnum> {

    public FlybyEnumState(Range<Time> horizon) {
      values = new HashMap<Range<Time>, FlybysEnum>() {{
        put(new Range<Time>(horizon.getMinimum(), Time.fromString("2025-180T00:00:00.000")), FlybysEnum.NOFLYBY);
        put(
            new Range<Time>(Time.fromString("2025-180T00:00:00.000"), Time.fromString("2025-185T00:00:00.000")),
            FlybysEnum.FIRSTFLYBY);
        put(
            new Range<Time>(Time.fromString("2025-185T00:00:00.000"), Time.fromString("2025-230T00:00:00.000")),
            FlybysEnum.NOFLYBY);
        put(
            new Range<Time>(Time.fromString("2025-230T00:00:00.000"), Time.fromString("2025-250T00:00:00.000")),
            FlybysEnum.SECONDFLYBY);
        put(
            new Range<Time>(Time.fromString("2025-250T00:00:00.000"), Time.fromString("2025-300T00:00:00.000")),
            FlybysEnum.NOFLYBY);
        put(
            new Range<Time>(Time.fromString("2025-300T00:00:00.000"), Time.fromString("2025-310T00:00:00.000")),
            FlybysEnum.THIRDFLYBY);
        put(new Range<Time>(Time.fromString("2025-310T00:00:00.000"), horizon.getMaximum()), FlybysEnum.NOFLYBY);
      }};
    }
  }

  /**
   * Hardcoded state describing some altitude state
   */
  public class AltitudeAboveMoon extends MockState<Integer> {

    public AltitudeAboveMoon(Range<Time> horizon) {
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


}
