package gov.nasa.jpl.ammos.mpsa.apgen.exceptions;

import java.lang.Exception;
import java.nio.file.Path;

public class FileNotReadableException extends Exception {

    private Path path;

    public FileNotReadableException(Path path) {
        super(String.format("Path %s does not exist or is not readable.", path.toString()));
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }
}
