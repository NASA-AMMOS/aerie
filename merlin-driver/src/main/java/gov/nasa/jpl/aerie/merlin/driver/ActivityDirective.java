package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public record ActivityDirective(Duration startOffset, SerializedActivity serializedActivity) {
  public ActivityDirective(final Duration startOffset, final String type, final Map<String, SerializedValue> arguments) {
    this(startOffset, new SerializedActivity(type, (arguments != null) ? Map.copyOf(arguments) : null));
  }
}
