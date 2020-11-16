package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import java.util.List;
import java.util.Map;

public interface TaskSpec {
  String getTypeName();
  Map<String, SerializedValue> getArguments();

  default List<String> getValidationFailures() {
    return List.of();
  }
}
