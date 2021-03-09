package gov.nasa.jpl.aerie.services.plan.utilities;

import gov.nasa.jpl.aerie.services.plan.models.AdaptationJar;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtils {
    public static Path getUniqueFilePath(final AdaptationJar adaptationJar, final Path basePath) {
        final String basename = adaptationJar.name;
        Path path = basePath.resolve(basename + ".jar");
        for (int i = 0; Files.exists(path); ++i) {
            path = basePath.resolve(basename + "_" + i + ".jar");
        }
        return path;
    }
}
