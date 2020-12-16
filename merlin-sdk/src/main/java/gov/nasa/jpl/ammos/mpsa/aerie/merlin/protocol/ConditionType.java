package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;

import java.util.Map;

public interface ConditionType<Condition> {
  String getName();
  Map<String, ValueSchema> getParameters();
  Map<String, SerializedValue> getArguments(Condition condition);

  Condition instantiate(Map<String, SerializedValue> arguments)
  throws UnconstructablelConditionException;

  class UnconstructablelConditionException extends Exception {}
}
