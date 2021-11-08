package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public final class UnexpectedMissingPlanException extends RuntimeException {
  private final String planId;

  public UnexpectedMissingPlanException(final String planId, final Throwable cause) {
    super("Plan with id `" + planId + "` is unexpectedly missing", cause);
    this.planId = planId;
  }

  public String getPlanId() {
    return this.planId;
  }
}
