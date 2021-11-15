package gov.nasa.jpl.aerie.merlin.server.utilities;

import gov.nasa.jpl.aerie.merlin.server.models.MissionModelJar;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtils {
    public static Path getUniqueFilePath(final MissionModelJar missionModelJar, final Path basePath) {
        final String basename = missionModelJar.name;
        Path path = basePath.resolve(basename + ".jar");
        for (int i = 0; Files.exists(path); ++i) {
            path = basePath.resolve(basename + "_" + i + ".jar");
        }
        return path;
    }
}
