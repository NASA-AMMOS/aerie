package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

public record PlanDatasetRecord(
    long planId,
    long datasetId,
    Duration offsetFromPlanStart) {}
