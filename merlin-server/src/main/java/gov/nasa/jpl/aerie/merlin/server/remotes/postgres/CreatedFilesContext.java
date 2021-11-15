package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/*package-local*/ final class CreatedFilesContext implements AutoCloseable {
  private final List<Path> paths = new ArrayList<>(1);
  private boolean committed = false;

  public void addPath(final Path path) {
    this.paths.add(path);
  }

  public void commit() {
    this.committed = true;
  }

  @Override
  public void close() throws DeleteUploadedFileException {
    if (this.committed) return;

    DeleteUploadedFileException baseEx = null;
    for (final var path : paths) {
      try {
        Files.delete(path);
      } catch (final IOException ex) {
        if (baseEx == null) {
          baseEx = new DeleteUploadedFileException(path, ex);
        } else {
          baseEx.addSuppressed(ex);
        }
      }
    }

    if (baseEx != null) throw baseEx;
  }
}
