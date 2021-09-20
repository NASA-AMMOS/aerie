package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import java.nio.file.Path;

public class CreateUploadedFileException extends IntegrationFailureException {
  public final Path source;
  public final Path target;

  public CreateUploadedFileException(final Path source, final Path target, final Throwable cause) {
    super("Could not copy uploaded file from %s to %s".formatted(source, target), cause);
    this.source = source;
    this.target = target;
  }
}
