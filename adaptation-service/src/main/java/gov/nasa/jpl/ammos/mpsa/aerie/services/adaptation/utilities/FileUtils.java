package gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.utilities;

import gov.nasa.jpl.ammos.mpsa.aerie.services.adaptation.models.AdaptationJar;

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
