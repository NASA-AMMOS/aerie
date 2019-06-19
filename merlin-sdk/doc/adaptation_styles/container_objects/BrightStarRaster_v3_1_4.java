package gov.nasa.jpl.clipper.uvs.calibration;

import gov.nasa.jpl.aerie.merlinsk.ActivityDefinition;
import gov.nasa.jpl.aerie.merlinsk.ActivityParameterDefinition;

/**
 * static factory class used to construct bright star raster activity definitions
 */
public class BrightStarRaster_v3_1_4 {
  /**
   * construct an activity definition for a bright star raster uvs calibration
   *
   * @return activity definition for bright star raster
   */
  static ActivityDefinition build() {
    new actDef = new ActivityDefinition();
   
    /*metadata*/
    General.applyMetadata( actDef );
    actDef.setName( "UvsBrightStarRasterCal" );
    actDef.setVersion( "3.1.4" );
    actDef.addLabels( "bsr" );
    actDef.setBrief( "calibrate the uvs via raster across a bright star" );
    actDef.setDocumentation(
      "scans the uvs airglow port across a field of view including one or "
      +"more well-characterized stars that are bright in the ultraviolet in "
      +"order to detect any alignment shifts in the uvs slit (for example due "
      +"to thrust eventst).\n"
      +"should occur at least once during uvs instrument comissioning."
      );
    actDef.addReferences( 
      "https://github.jpl.nasa.gov/Europa/OPS/blob/0bc8a70f29d4c74e27b714f1c67c359ce8d9ea14/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L545"
      );
    
    /*parameters*/
    ActivityParameterDefinition pDef = CelestialCoordinates.build();
    pDef.setName( "target" );
    pDef.setBrief( "coordinates of the calibration target to use" );
    pDef.setDocumentation(
      "points to the center of the star field across which the uvs should "
      +"scan the airglow port"
      );
    actDef.addParameter( pDef );

    return actDef;
  }
  
}
