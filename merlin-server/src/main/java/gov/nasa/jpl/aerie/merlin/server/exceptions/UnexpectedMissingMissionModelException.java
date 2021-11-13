package gov.nasa.jpl.aerie.merlin.server.exceptions;

public class UnexpectedMissingMissionModelException extends RuntimeException {
  private final String adaptationId;

  public UnexpectedMissingMissionModelException(final String adaptationId, final Throwable cause) {
    super("Adaptation with id `" + adaptationId + "` is unexpectedly missing", cause);
    this.adaptationId = adaptationId;
  }

  public String getAdaptationId() {
    return this.adaptationId;
  }
}
