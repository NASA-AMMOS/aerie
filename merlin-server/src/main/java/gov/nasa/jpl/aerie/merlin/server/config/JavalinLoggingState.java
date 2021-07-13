package gov.nasa.jpl.aerie.merlin.server.config;

public enum JavalinLoggingState {
  Enabled, Disabled;

  public boolean isEnabled() {
    return (this == Enabled);
  }
}
