package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Optional;

public final record SimulationTemplateRecord(
    long id,
    long revision,
    long modelId,
    String description,
    Map<String, SerializedValue> arguments) {}

