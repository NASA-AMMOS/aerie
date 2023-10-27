package gov.nasa.jpl.aerie.merlin.protocol.types;

import java.util.Map;

public sealed interface DurationType {
  record Controllable(String parameterName, ControllableDurationValueMapper valueMapperInterface) implements DurationType {}
  record Uncontrollable() implements DurationType {}
  record Fixed(Duration duration) implements DurationType {}
  record Parametric(ThrowingDurationFunction durationFunction) implements DurationType {}

  static DurationType uncontrollable() {
    return new Uncontrollable();
  }

  static DurationType controllable(final String parameterName, final ControllableDurationValueMapper durationValueMapper) {
    return new Controllable(parameterName, durationValueMapper);
  }

  static DurationType fixed(final Duration duration) {
    return new Fixed(duration);
  }

  static DurationType parametric(final ThrowingDurationFunction durationFunction) {
    return new Parametric(durationFunction);
  }

  interface ControllableDurationValueMapper {
    SerializedValue serializeControllableDuration(final Duration duration);
    Duration deserializeControllableDuration(final SerializedValue serializedValue);
  }

  interface ThrowingDurationFunction {
    Duration apply(final Map<String, SerializedValue> arguments) throws InstantiationException;
  }
}
