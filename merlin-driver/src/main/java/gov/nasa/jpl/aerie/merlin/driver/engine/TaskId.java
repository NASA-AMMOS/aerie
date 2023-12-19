package gov.nasa.jpl.aerie.merlin.driver.engine;

import java.util.UUID;

/** A typed wrapper for task IDs. */
public record TaskId(String id) {
  public static TaskId generate(String displayName) {
    return new TaskId(displayName + UUID.randomUUID().toString());
  }
}
