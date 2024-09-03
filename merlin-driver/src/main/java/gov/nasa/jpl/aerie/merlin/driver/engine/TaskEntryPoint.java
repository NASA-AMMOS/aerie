package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.types.SerializedActivity;

import java.util.UUID;

public sealed interface TaskEntryPoint {
  String id();

  record Directive(String id, SerializedActivity directive) implements TaskEntryPoint {}
  record Daemon(String id) implements TaskEntryPoint {}
  record SystemTask(String id) implements TaskEntryPoint {}
  record Subtask(String id, String parentId, int childNumber) implements TaskEntryPoint {}

  static String freshId() {
    return String.valueOf(System.currentTimeMillis()) + UUID.randomUUID();
  }
}
