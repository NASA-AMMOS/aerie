package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import java.util.Map;
import java.util.Optional;

public final record SimulationRecord(
    long id,
    long revision,
    PlanId planId,
    Optional<Long> simulationTemplateId,
    Map<String, SerializedValue> arguments) {}
