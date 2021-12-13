package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public final record PlanDatasetRecord(
    long planId,
    long datasetId,
    Duration offsetFromPlanStart) {}
