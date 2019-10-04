package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions;

import java.nio.file.Path;

public class InvalidAdaptationJARException extends Exception {
    private final Path path;

    public InvalidAdaptationJARException(final Path path) {
        this.path = path;
    }

    public InvalidAdaptationJARException(final Path path, final Throwable cause) {
        super(cause);
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }
}
