package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Optional;

public record Constraint (Long id, Long revision, Optional<Long> invocationId, Map<String, SerializedValue> arguments, String name, String description, String definition) {
  public Constraint(Long id, Long revision, String name, String description, String definition) {
    this(id, revision, Optional.empty(), Map.of(), name, description, definition);
  }
  public Constraint(Long id, Long revision, Long invocationId, Map<String, SerializedValue> arguments, String name, String description, String definition) {
    this(id, revision, Optional.of(invocationId), arguments, name, description, definition);
  }
}
