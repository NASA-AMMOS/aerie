package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.List;
import java.util.Map;

public interface TaskSpecType<$Schema, AdaptationTaskSpec> {
  String getName();
  Map<String, ValueSchema> getParameters();

  AdaptationTaskSpec instantiateDefault();
  AdaptationTaskSpec instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructableTaskSpecException;

  Map<String, SerializedValue> getArguments(AdaptationTaskSpec taskSpec);
  List<String> getValidationFailures(AdaptationTaskSpec taskSpec);

  <$Timeline extends $Schema> Task<$Timeline> createTask(AdaptationTaskSpec taskSpec);

  class UnconstructableTaskSpecException extends Exception {}
}
