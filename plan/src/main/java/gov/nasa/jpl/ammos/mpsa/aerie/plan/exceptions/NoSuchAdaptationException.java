package gov.nasa.jpl.ammos.mpsa.aerie.plan.exceptions;

public class NoSuchAdaptationException extends Exception {
  private final String adaptationId;

  public NoSuchAdaptationException(final String adaptationId) {
    super("No adaptation exists with id `" + adaptationId + "`");
    this.adaptationId = adaptationId;
  }

  public String getInvalidAdaptationId() {
    return this.adaptationId;
  }
}
