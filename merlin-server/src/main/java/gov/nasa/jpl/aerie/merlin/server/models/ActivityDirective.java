package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ActivityDirective {
  public String type;
  public Duration startOffset;
  public Map<String, SerializedValue> arguments;

  public ActivityDirective() {}

  public ActivityDirective(final ActivityDirective other) {
    this.type = other.type;
    this.startOffset = other.startOffset;
    this.arguments = (other.arguments == null) ? null : new HashMap<>(other.arguments);
  }

  public ActivityDirective(final String type, final Duration startOffset, final Map<String, SerializedValue> arguments) {
    this.type = type;
    this.startOffset = startOffset;
    this.arguments = (arguments != null) ? Map.copyOf(arguments) : null;
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof ActivityDirective)) {
      return false;
    }

    final var other = (ActivityDirective)object;
    return
        (  Objects.equals(this.type, other.type)
        && Objects.equals(this.startOffset, other.startOffset)
        && Objects.equals(this.arguments, other.arguments)
        );
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, startOffset, arguments);
  }
}
