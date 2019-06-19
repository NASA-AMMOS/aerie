package gov.nasa.jpl.clipper.uvs.calibration;

import gov.nasa.jpl.aerie.merlinsdk.Activity;
import gov.nasa.jpl.aerie.merlinsdk.annotations.ActivityType;

public class BrightStarRaster_v3_1_4 implements Activity {
  private CelestialCoordinates target;

  /**
   * Scans the uvs airglow port across a field of view including one or
   * more well-characterized stars that are bright in the ultraviolet in
   * order to detect any alignment shifts in the uvs slit (for example due
   * to thrust events).
   * 
   * Should occur at least once during uvs instrument comissioning.
   *
   * @brief calibrate the uvs via raster across a bright star
   * @contact Steve Schaffer (steve.schaffer@jpl.nasa.gov)
   * @subsystem UVS
   * @stakeholders UVS, GNC
   * @campaign UVS.calibration
   * @labels uvs, cal, bsr
   * @see https://github.jpl.nasa.gov/Europa/OPS/blob/0bc8a70f29d4c74e27b714f1c67c359ce8d9ea14/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L545
   * @see https://charlie-lib.jpl.nasa.gov/docushare/dsweb/Get/Document-2165712/UVS%20Instrument%20Ops%20Summary%202017-01-13.docx
   * @version 3.1.4
   * @param target Vector to the center of the star field across which the uvs
       should scan the airglow port.
   */
  @ActivityType("BrightStarRaster")
  public BrightStarRaster_v3_1_4(Optional<CelestialCoordinates> target) {
    this.target = target.orElse(new CelestialCoordinates());
  }

  public CelestialCoordinates getTarget() { return target; }
}
