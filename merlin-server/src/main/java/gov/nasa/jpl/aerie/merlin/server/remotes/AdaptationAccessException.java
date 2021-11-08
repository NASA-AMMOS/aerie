package gov.nasa.jpl.aerie.merlin.server.remotes;

import java.nio.file.Path;

public class AdaptationAccessException extends RuntimeException {
  private final Path path;

  public AdaptationAccessException(final Path path, final Throwable cause) {
    super(cause);
    this.path = path;
  }

  public Path getPath() {
    return this.path;
  }
}
