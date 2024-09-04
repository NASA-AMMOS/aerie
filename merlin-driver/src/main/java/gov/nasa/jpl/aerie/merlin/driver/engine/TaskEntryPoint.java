package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.types.SerializedActivity;

import java.util.Optional;
import java.util.UUID;

public sealed interface TaskEntryPoint {
  String id();
  Optional<ParentReference> parent();

  record Directive(String id, SerializedActivity directive, Optional<ParentReference> parent) implements TaskEntryPoint {}
  record Daemon(String id) implements TaskEntryPoint {
    @Override
    public Optional<ParentReference> parent() {
      return Optional.empty();
    }
  }
  record SystemTask(String id) implements TaskEntryPoint {
    @Override
    public Optional<ParentReference> parent() {
      return Optional.empty();
    }
  }
  record Subtask(String id, ParentReference parent$) implements TaskEntryPoint {
    @Override
    public Optional<ParentReference> parent() {
      return Optional.of(parent$);
    }
  }

  static String freshId() {
    return String.valueOf(System.currentTimeMillis()) + UUID.randomUUID();
  }

  record ParentReference(String id, long childNumber) {}
}
