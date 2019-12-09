package gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk;

import java.nio.file.Path;

public class MissingAdaptationException extends Exception {
    private final Path adaptationPath;

    public MissingAdaptationException(final Path adaptationPath) {
        super("No Adaptation exists in the resource at path `" + adaptationPath + "`");
        this.adaptationPath = adaptationPath;
    }

    public Path getAdaptationPath() {
        return this.adaptationPath;
    }
}
