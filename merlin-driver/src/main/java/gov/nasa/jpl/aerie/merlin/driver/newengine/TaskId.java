package gov.nasa.jpl.aerie.merlin.driver.newengine;

import java.util.UUID;

/** A typed wrapper for task IDs. */
/*package-local*/ record TaskId(String id) {
  public static TaskId generate() {
    return new TaskId(UUID.randomUUID().toString());
  }
}
