package gov.nasa.jpl.clipper.uvs.calibration;

import gov.nasa.jpl.aerie.merlinsdk.annotations.ParameterType;

/**
 * pointing to an inertial fixed location in the sky
 *
 * used as a parameter to several activities
 */
public class CelestialCoordinates {
  private float ra_rad;
  private float dec_rad;
  private String referenceFrame;

  /**
   * Construct a default celestial coordinate, pointing toward the north celestial pole.
   *
   * @param ra_rad The angle measured in radians eastward from the sun at the specified
   *   epoch vernal equinox along the celestial equator to the hour cicle that passes
   *   through the target point; essentially celestial longitude. Must be between
   *   2pi and -2pi.
   * @param dec_rad The angle measured in radians north of the celestial equator
   *   at the specified epoch time along the hour circle that passes through the
   *   target point; essentially celestial latitude.
   * @param referenceFrame The frame of reference (and epoch, if needed) used to
   *   measure the target pointing coordinates. Examples: "J2000.0", "ICRS".
   */
  @ParameterType
  public CelestialCoordinates(
    Optional<Float> ra_rad,
    Optional<Float> dec_rad,
    Optional<String> referenceFrame
  ) {
    this.ra_rad = ra_rad.orElse(0.0);
    this.dec_rad = ra_rad.orElse(Math.PI/2);
    this.referenceFrame = referenceFrame.orElse("J2000");

    if (Math.abs(this.ra_rad) > 2*java.lang.Math.PI) {
      throw new IllegalArgumentException("Specified declination_rad value=" + dec_rad + " out of range [-pi/2,+pi/2]");
    }
  }

  // Default constructor for convenience
  public CelestialCoordinates() {
    this(Optional.empty(), Optional.empty(), Optional.empty());
  }

  public float getRaRad() { return ra_rad; }
  public float getDecRad() { return dec_rad; }
  public float getReferenceFrame() { return referenceFrame; }
}
