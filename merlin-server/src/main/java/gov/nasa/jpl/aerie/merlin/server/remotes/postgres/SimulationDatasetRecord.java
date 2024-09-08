package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;


import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.types.Timestamp;

import java.util.Map;

public record SimulationDatasetRecord(
    long simulationId,
    long datasetId,
    SimulationStateRecord state,
    boolean canceled,
    Timestamp simulationStartTime,
    Timestamp simulationEndTime,
    long simulationDatasetId,
    Map<String, SerializedValue> arguments) {}
