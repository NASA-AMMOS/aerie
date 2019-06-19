package gov.nasa.jpl.clipper.uvs.calibration;

import gov.nasa.jpl.aerie.merlinsk.ActivityParameterDefinition;
import gov.nasa.jpl.aerie.merlinsk.StructActivityParameterDefinition;
import gov.nasa.jpl.aerie.merlinsk.PrimitiveActivityParameterDefinition;
import gov.nasa.jpl.aerie.merlinsk.Validators;

/**
 * static factory class used to build celestial coordinate parameter definitions
 */
public class CelestialCoordinates {
  /**
   * construct a parameter definition that holds a celestial coordinate
   */
  static ActivityParameterDefinition build() {    
    structDef = new StructActivityParameterDefinition();

    structDef.addParameter( buildRaParamDef() );
    structDef.addParameter( buildDecParamDef() );
    structDef.addParameter( buildFrameParamDef() );
    
    return structDef;
  }
  
  /**
   * create a parameter definition for right ascension
   */
  static ActivityParameterDefinition buildRaParamDef() {
    ActivityParameterDefinition paramDef = new PrimitiveActivityParameterDefinition(
      Double.class );
    paramDef.setName( "rightAscension" );
    paramDef.setUnits( "radians" );
    paramDef.setDimension( "angle" );
    paramDef.setDefaultValue( 0.0 );
    paramDef.setBrief( "the target pointing declination angle, in radians" );
    paramDef.setDocumentation( 
      "the angle measured in radians north of the celestial equator at "
      +" the specified epoch time along the hour circle that passes through "
      +" the target point; essentially celestial latitude"
      );
    
    //add basic validation from a library of validators in the sdk
    paramDef.addValidation( Validators.inRange( -Math.PI, Math.PI ) );
    
    return paramDef;        
  }

  /**
   * create a parameter definition for declination
   */
  static ActivityParameterDefinition buildDecParamDef() {
    ActivityParameterDefinition paramDef = new PrimitiveActivityParameterDefinition(
      Double.class );
    paramDef.setName( "declination" );
    paramDef.setUnits( "radians" );
    paramDef.setDimension( "angle" );
    paramDef.setDefaultValue( Math.PI/2 );
    paramDef.setBrief( "the target pointing declination angle, in radians" );
    paramDef.setDocumentation( 
      "the angle measured in radians north of the celestial equator at "
      +" the specified epoch time along the hour circle that passes through "
      +" the target point; essentially celestial latitude"
      );

    //add custom validation code via lambda (or method ref, or consumer, etc)
    paramDef.addValidation( v->{ if( v<-Math.PI/2 || v>Math.PI/2 ) { 
          throw new ParameterValidationException(
            "declination="+v+" out of range [-pi/2,+pi/2]"); } } );
    
    return paramDef;    
  }

  /**
   * create a parameter definition for declination
   */
  static ActivityParameterDefinition buildFrameParamDef() {
    ActivityParameterDefinition paramDef = new PrimitiveActivityParameterDefinition(
      String.class );
    paramDef.setName( "referenceFrame" );
    paramDef.setUnits( "none" );
    paramDef.setDimension( "reference_frame_name" );
    paramDef.setDefaultValue( "J2000.0" );
    paramDef.setDocumentation(
      "the frame of reference (and epoch, if needed) used to measure the "
      +"target pointing coordinates, for example J2000.0 or ICRS"
      );
    return paramDef;
  }

}



