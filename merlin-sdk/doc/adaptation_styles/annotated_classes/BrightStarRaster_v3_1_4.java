//NB: paths on disk must meet java tool conventions for packages
package gov.nasa.jpl.clipper.uvs.calibration;

//pull in annotations provided by the merlinsdk
import gov.nasa.jpl.aerie.merlinsdk.annotations.*;

/**
 * models calibration of the uvs via rastering across a bright star
 */
//registers the class as an activity definition in the system
//overrides name automatically infered the java class decl to allow versioning
//user-visible documentation strings in annotations (can't reflect)
//adapter-visible documentation in javadoc
//various metadata available to help direct eg email notices etc
@ActivityDefinition{ brief="calibrate the uvs via raster across a bright star",
    name="BrightStarRaster", 
    version="3.1.4",
    subsystem="UVS",
    contacts={"steve.schaffer@jpl.nasa.gov"},
    stakeholders={"UVS", //main instrument
                  "GNC"}, //need to repoint
    campaign="UVS.calibration",
    labels={"uvs","cal","bsr"},
    doc="scans the uvs airglow port across a field of view including one or "
      +"more well-characterized stars that are bright in the ultraviolet in "
      +"order to detect any alignment shifts in the uvs slit (for example due "
      +"to thrust eventst).\n"
      +"should occur at least once during uvs instrument comissioning.",
    refs={"https://github.jpl.nasa.gov/Europa/OPS/blob/0bc8a70f29d4c74e27b714f1c67c359ce8d9ea14/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L545",
          "https://charlie-lib.jpl.nasa.gov/docushare/dsweb/Get/Document-2165712/UVS%20Instrument%20Ops%20Summary%202017-01-13.docx"}
}
public class BrightStarRaster_v3_1_4 implements Activity {

  /**
   * pointing to the bright target that the uvs should raster across
   */
  //designate as an input parameter to the activity (vs locals)
  //automatically infers type and name from java field decl, or override
  //default value from default ctor
  @Parameter{ brief="coordinates of the calibration target to use",
      doc="points to the center of the star field across which the uvs should "
      +"scan the airglow port",
      dimension="inertial_pointing"
      }
  CelestialCoordinates target;
  
  /**
   * general constraints required for all uvs calibrations
   */
  //reference to packet of reusable constraints
  @Constraint{ brief="general constraints required for all uvs calibrations"}
  ConstraintGroup uvsCalibrationConstraints;

  
  
}
