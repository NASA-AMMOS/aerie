package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public final record SimulationRecord(
    long id,
    long revision,
    long planId,
    Map<String, SerializedValue> arguments) {}
