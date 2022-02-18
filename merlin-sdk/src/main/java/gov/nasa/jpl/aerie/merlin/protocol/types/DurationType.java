package gov.nasa.jpl.aerie.merlin.protocol.types;

public sealed interface DurationType {
  record Uncontrollable() implements DurationType {}

  static DurationType uncontrollable() {
    return new Uncontrollable();
  }
}
