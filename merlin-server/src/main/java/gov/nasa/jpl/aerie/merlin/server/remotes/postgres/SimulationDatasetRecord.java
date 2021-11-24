package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public final record SimulationDatasetRecord(
    long simulationId,
    long datasetID,
    long simulationRevision,
    long planRevision,
    long modelRevision,
    SimulationStateRecord state,
    boolean canceled,
    Duration offsetFromPlanStart) {}
