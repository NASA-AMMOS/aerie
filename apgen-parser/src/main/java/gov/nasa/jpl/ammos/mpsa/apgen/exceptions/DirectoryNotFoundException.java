package gov.nasa.jpl.ammos.mpsa.apgen.exceptions;

import java.lang.Exception;
import java.nio.file.Path;

public class DirectoryNotFoundException extends Exception {

    private Path path;

    public DirectoryNotFoundException(Path path) {
        super(String.format("Directory %s does not exist", path.toString()));
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }
}
