package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public final record DatasetMetadataRecord(
    long simulationId,
    long datasetID,
    long simulationRevision,
    long planRevision,
    long modelRevision) {}
