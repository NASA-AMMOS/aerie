package gov.nasa.jpl.clipper.uvs.calibration;

import java.io.FileReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import gov.nasa.jpl.aerie.merlinsdk.Adaptation;

/**
 * static factory class builds complete adaptation
 */
public class ClipperAdaptation_v1_2_3 {
  /**
   * construct the complete adaptation
   */
  static build()
  {
    Adaptation adap = new Adaptation();
    
    /*activity defs*/
    adap.addActivityDefinition( BrightStarRaster_v3_1_4.build() );
    adap.addActivityDefinition( BrightStarStare_v2_7_1.build() );
    adap.addActivityDefinition( ExtendedSource_v1_6_1.build() );
    adap.addActivityDefinition( Observation_v1_4_1.build() );
    
    return adap;
  }

  /**
   * parses a given dictionary file to form a complete adaptation
   */
  static parse( String filepath ) {
    Adaptation adap = new Adaptation();
    
    JSONObject json = (JSONObject) new org.json.simple.JSONParser()
      .parse( new FileReader( filepath ) );
    
    JSONArray actDefJsons = (JSONArray) json.get("ActivityDefinitions");
    Iterator actDefJsonI = actDefJsons.iterator();
    while( actDefJsonI.hasNext() ) {
      
      adap.addActivityDefinition( convert( actDefJsonI.next() ) );

    }
    
    return adap;
  }
}
