package gov.nasa.jpl.aerie.merlin.protocol.types;

public sealed interface DurationType {
  record Controllable(String parameterName) implements DurationType {}
  record Uncontrollable() implements DurationType {}
  record Fixed(Duration duration) implements DurationType {}

  static DurationType uncontrollable() {
    return new Uncontrollable();
  }

  static DurationType controllable(final String parameterName) {
    return new Controllable(parameterName);
  }

  static DurationType fixed(final Duration duration) {
    return new Fixed(duration);
  }
}
