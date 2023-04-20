package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Optional;

public record PlanDatasetRecord(
    long planId,
    Optional<Long> simulationDatasetId,
    long datasetId,
    Duration offsetFromPlanStart) {}
