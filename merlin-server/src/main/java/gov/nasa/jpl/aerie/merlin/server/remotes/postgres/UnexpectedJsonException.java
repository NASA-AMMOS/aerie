package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public class UnexpectedJsonException extends IntegrationFailureException {
  public UnexpectedJsonException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
