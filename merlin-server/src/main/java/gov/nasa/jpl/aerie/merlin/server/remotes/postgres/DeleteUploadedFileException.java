package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import java.nio.file.Path;

public class DeleteUploadedFileException extends IntegrationFailureException {
  public final Path target;

  public DeleteUploadedFileException(final Path target, final Throwable cause) {
    super("Could not delete uploaded file at %s".formatted(target), cause);
    this.target = target;
  }
}
