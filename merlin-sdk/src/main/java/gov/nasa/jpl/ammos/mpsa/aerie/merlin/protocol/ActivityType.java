package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.Map;

public interface ActivityType<Activity extends ActivityInstance> {
  String getName();
  Map<String, ValueSchema> getParameters();

  Activity instantiateDefault();

  Activity instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableActivityException;

  class UnconstructableActivityException extends Exception {}
}
