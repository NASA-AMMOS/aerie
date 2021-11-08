package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public final class NoSuchDatasetException extends Exception {
  public final long datasetId;

  public NoSuchDatasetException(final long datasetId) {
    super(String.format("No dataset with id `%s` exists", datasetId));
    this.datasetId = datasetId;
  }
}
