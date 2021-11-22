package gov.nasa.jpl.aerie.scheduler.server.config;

/**
 * controls the requested verbosity of logging by javalin endpoints
 */
public enum JavalinLoggingState {

  /**
   * request javalin developer-level logging, as per JavalinConfig#enableDevLogging()
   */
  Enabled,

  /**
   * leaves default javalin logging
   */
  Disabled
}
