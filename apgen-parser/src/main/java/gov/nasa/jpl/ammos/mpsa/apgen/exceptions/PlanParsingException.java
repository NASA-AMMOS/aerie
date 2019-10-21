package gov.nasa.jpl.ammos.mpsa.apgen.exceptions;

import java.nio.file.Path;

public class PlanParsingException extends Exception {
    private Path path;

    public PlanParsingException(Path path, String reason) {
        super(String.format("Plan file %s could not be parsed: %s.", path.toString(), reason));
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }
}
