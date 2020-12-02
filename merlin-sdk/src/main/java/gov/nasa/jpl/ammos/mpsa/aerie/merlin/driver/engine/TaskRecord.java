package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import java.util.Map;
import java.util.Optional;

public final class TaskRecord {
  public final String type;
  public final Map<String, SerializedValue> arguments;
  public final Optional<String> parentId;

  public TaskRecord(
      final String type,
      final Map<String, SerializedValue> arguments,
      final Optional<String> parentId
  )
  {
    this.type = type;
    this.arguments = arguments;
    this.parentId = parentId;
  }
}
