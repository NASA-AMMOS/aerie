package gov.nasa.jpl.aerie.merlin.framework.htn;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public record ActivityInstantiation(ActivityReference reference, Map<String, Object> parameters) {
  public static ActivityInstantiation of(ActivityReference reference, Map<String, Object> parameters){
    return new ActivityInstantiation(reference, parameters);
  }
}
