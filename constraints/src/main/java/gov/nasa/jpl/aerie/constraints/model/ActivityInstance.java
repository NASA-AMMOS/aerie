package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;

import java.util.Map;

public final class ActivityInstance {
  public final String id;
  public final String type;
  public final Map<String, SerializedValue> parameters;
  public final Window window;

  public ActivityInstance(
      final String id,
      final String type,
      final Map<String, SerializedValue> parameters,
      final Window window
  ) {
    this.type = type;
    this.id = id;
    this.parameters = parameters;
    this.window = window;
  }
}
