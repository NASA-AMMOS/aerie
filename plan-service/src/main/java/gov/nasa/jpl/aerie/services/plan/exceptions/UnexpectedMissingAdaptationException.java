package gov.nasa.jpl.aerie.services.plan.exceptions;

public class UnexpectedMissingAdaptationException extends RuntimeException {
  private final String adaptationId;

  public UnexpectedMissingAdaptationException(final String adaptationId, final Throwable cause) {
    super("Adaptation with id `" + adaptationId + "` is unexpectedly missing", cause);
    this.adaptationId = adaptationId;
  }

  public String getAdaptationId() {
    return this.adaptationId;
  }
}
