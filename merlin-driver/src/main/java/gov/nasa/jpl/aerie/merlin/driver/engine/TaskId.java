package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;

import java.util.UUID;

/** A typed wrapper for task IDs. */
public record TaskId(String id) implements Scheduler.TaskIdentifier {
  public static TaskId generate() {
    return new TaskId(UUID.randomUUID().toString());
  }
}
