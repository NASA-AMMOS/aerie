package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ActivityInstance {
  public String type;
  public Timestamp startTimestamp;
  public Map<String, SerializedValue> arguments;

  public ActivityInstance() {}

  public ActivityInstance(final ActivityInstance other) {
    this.type = other.type;
    this.startTimestamp = other.startTimestamp;
    this.arguments = (other.arguments == null) ? null : new HashMap<>(other.arguments);
  }

  public ActivityInstance(final String type, final Timestamp startTimestamp, final Map<String, SerializedValue> arguments) {
    this.type = type;
    this.startTimestamp = startTimestamp;
    this.arguments = (arguments != null) ? Map.copyOf(arguments) : null;
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof ActivityInstance)) {
      return false;
    }

    final var other = (ActivityInstance)object;
    return
        (  Objects.equals(this.type, other.type)
        && Objects.equals(this.startTimestamp, other.startTimestamp)
        && Objects.equals(this.arguments, other.arguments)
        );
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, startTimestamp, arguments);
  }
}
