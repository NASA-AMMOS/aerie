package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public record ConstraintRecord(
    long id,
    long revision,
    long invocationId,
    String name,
    String description,
    String definition,
    Map<String, SerializedValue> arguments) {}
