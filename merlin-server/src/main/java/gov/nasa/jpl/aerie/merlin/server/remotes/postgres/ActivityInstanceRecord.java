package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public record ActivityInstanceRecord(
    long id,
    String type,
    long startOffsetInMicros,
    Map<String, SerializedValue> arguments) {}
