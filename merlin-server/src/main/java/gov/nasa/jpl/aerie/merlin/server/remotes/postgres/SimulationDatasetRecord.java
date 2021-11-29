package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public final record SimulationDatasetRecord(
    long simulationId,
    long datasetID,
    long simulationRevision,
    long planRevision,
    long modelRevision,
    String state,
    String reason,
    boolean canceled) {}
