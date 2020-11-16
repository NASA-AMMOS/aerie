package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.Map;

public interface TaskSpecType<AdaptationTaskSpec extends TaskSpec> {
  String getName();
  Map<String, ValueSchema> getParameters();

  AdaptationTaskSpec instantiateDefault();

  AdaptationTaskSpec instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException;

  class UnconstructableTaskSpecException extends Exception {}
}
