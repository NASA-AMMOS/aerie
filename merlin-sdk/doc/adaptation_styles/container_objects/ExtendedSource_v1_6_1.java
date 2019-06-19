package gov.nasa.jpl.clipper.uvs.calibration;


import gov.nasa.jpl.aerie.merlinsk.ActivityDefinition;
import gov.nasa.jpl.aerie.merlinsk.ActivityParameterDefinition;
import gov.nasa.jpl.aerie.merlinsk.BasicParameters;
import gov.nasa.jpl.clipper.nav.SolarSystemBodies;

/**
 * static factory class constructs extended source calbiration activity definition
 */
public class ExtendedSource_v1_6_1 {
  /**
   * construct an activity definition for an extended source uvs calibration
   *
   * @return activity definition for uvs extended source calibration
   */
  static ActivityDefinition build() {
    new actDef = new ActivityDefinition();

    /*metadata*/
    General.applyMetadata( actDef );
    actDef.setName( "UvsExtendedSourceCal" );
    actDef.setVersion( "1.6.1" );
    actDef.addLabels( "es" );
    actDef.setBrief( "calibrate the uvs airglow port by pointing to jupiter" );
    actDef.setDocumentation(
      "points the uvs at a non-point source of ultraviolet light (typically "
      +" the disc of jupiter while it is sufficiently illuminated by the sun) "
      +" and collect air-glow port photon data from the source in order to "
      +" characterize the air-glow slit's optical throughput (in combination "
      +" with prior information of the point spread function and slit shape).\n"
      +"should occur once during tour"
      );
    actDef.addReferences( 
      "https://github.jpl.nasa.gov/Europa/OPS/blob/clipper-develop/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L695"
      );
    
    /*parameters*/
    ActivityParameterDefinition pDef = BasicParameters.buildEnum( 
      SolarSystemBodies.class );
    pDef.setName( "targetBody" );
    pDef.setDefaultValue( SolarSystemBodies.Jupiter );
    pDef.setBrief( "target body for uvs to point at during calibration" );
    pDef.setDocumentation(
      "the target body that the uvs should point at and track continuously "
      +" during the extended source calibration, selected from the set of "
      +" solar system bodies in the navigation frame tree"
      );
    actDef.addParameter( pDef );
    
    return actDef;
  }
  
}
