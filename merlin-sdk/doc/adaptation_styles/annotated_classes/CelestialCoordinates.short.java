package gov.nasa.jpl.clipper.uvs.calibration;

import gov.nasa.jpl.aerie.merlinsdk.annotations.*;

/**
 * pointing to an inertial fixed location in the sky
 *
 * used as a parameter to several activities
 */
@ParameterType
public class CelestialCoordinates {

  /**
   * the target pointing right ascension angle, in radians, at specified epoch
   */
  @Parameter{ brief="the target pointing right ascension angle, in radians",
      name="rightAscension_rad",
      units="radians",
      dimension="angle",
      min=-2*java.lang.Math.PI,
      max=2*java.lang.Math.PI,
      doc="the angle measured in radians eastward from the sun at the "
      +" specified epoch vernal equinox along the celestial equator to the "
      +" hour cicle that passes through the target point; essentially "
      +" celestial longitude"
      }
  float ra_rad;


  /**
   * the target pointing declination angle, in radians, at specified epoch
   */
  @Parameter{ brief="the target pointing declination angle, in radians" ,
      name="declination_rad",
      units="radians",
      dimension="angle",
      doc="the angle measured in radians north of the celestial equator at "
      +" the specified epoch time along the hour circle that passes through "
      +" the target point; essentially celestial latitude"
      }
  float dec_rad;


  /**
   * ensure that declination is within acceptable [-pi/2,+pi/2] range
   *
   * @throws ParameterValidationException iff the declination field is
   *         outside of legal range
   */
  @Validation{parameters={"declination_rad"}}
  void declinationInMinMax() throws ParameterValidationException {
    if( dec_rad > java.lang.Math.PI/2 
        || dec_rad < java.lang.Math.PI/2 ) {
      throw new ParameterValidationException( 
        "Specified declination_rad value="+dec_rad
        +" out of range [-pi/2,+pi/2]");
    }
  }


  /**
   * the frame of reference in which the measurements are made, including epoch
   *
   * for example J2000.0 or ICRS
   */
  @Parameter{ brief="the astronomical reference frame and epoch of the coordinates",
      units="none",
      dimension="reference_frame_name",
      doc="the frame of reference (and epoch, if needed) used to measure the "
      +"target pointing coordinates, for example J2000.0 or ICRS"}
  String referenceFrame;


  /**
   * construct a default celestial coordinate, pointing toward the north celestial pole
   */
  public CelestialCoordinates() {
    ra_rad = 0.0;
    dec_rad = java.lang.Math.PI/2;
    epoch = "J2000";
  }



}
