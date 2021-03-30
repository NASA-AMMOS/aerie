package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;

import java.util.Map;
import java.util.Objects;

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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ActivityInstance)) return false;
    final var o = (ActivityInstance)obj;

    return Objects.equals(this.id, o.id) &&
           Objects.equals(this.type, o.type) &&
           Objects.equals(this.parameters, o.parameters) &&
           Objects.equals(this.window, o.window);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.type, this.parameters, this.window);
  }
}
