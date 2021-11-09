package gov.nasa.jpl.aerie.apgen.exceptions;

import java.lang.Exception;
import java.nio.file.Path;

public class APGenAdaptationParsingException extends Exception {

    private Path path;

    public APGenAdaptationParsingException(Path path, String reason) {
        super(String.format("Adaptation file %s could not be parsed: %s.", path.toString(), reason));
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }
}
