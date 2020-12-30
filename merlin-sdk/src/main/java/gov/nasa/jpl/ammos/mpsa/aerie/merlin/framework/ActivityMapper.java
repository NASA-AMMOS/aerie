package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.List;
import java.util.Map;

public interface ActivityMapper<Instance> {
  String getName();
  Map<String, ValueSchema> getParameters();
  Map<String, SerializedValue> getArguments(Instance activity);

  Instance instantiateDefault();
  Instance instantiate(Map<String, SerializedValue> arguments) throws TaskSpecType.UnconstructableTaskSpecException;

  List<String> getValidationFailures(Instance activity);
}
