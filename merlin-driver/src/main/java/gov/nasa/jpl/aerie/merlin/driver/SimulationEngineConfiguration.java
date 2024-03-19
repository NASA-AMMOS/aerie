package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.time.Instant;
import java.util.Map;

public record SimulationEngineConfiguration(
    Map<String, SerializedValue> simulationConfiguration,
    Instant simStartTime,
    MissionModelId missionModelId
) {}
