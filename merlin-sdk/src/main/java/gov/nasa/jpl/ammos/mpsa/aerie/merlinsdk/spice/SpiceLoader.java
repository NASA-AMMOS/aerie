package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// Inspired by:
// https://stackoverflow.com/questions/12036607/bundle-native-dependencies-in-runnable-jar-with-maven
public final class SpiceLoader {
    private SpiceLoader() {}

    public static void loadSpice() {
        final String library = "JNISpice";

        // Attempt to load our copy of JNISpice via `System.load`.
        try (final InputStream in = getResourceByOS().openStream()) {
            // Copy the JNISpice library to a temporary location on-disk, then load it into the JVM.
            final Path file = Files.createTempFile(library + "-", ".tmp");
            deleteFileOnExit(file);

            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);

            System.load(file.toString());
            return;
        } catch (final IOException ex) {
            // Swallow the error; fall through and try a different approach.
        }

        // Attempt to load a host-installed copy of JNISpice via `System.loadLibrary`.
        System.loadLibrary(library);
    }

    private static URL getResourceByOS() {
        final String resourcePath;
        {
            final String osName = System.getProperty("os.name").toLowerCase();
            if (osName.startsWith("win")) {
                resourcePath = "/gov/nasa/jpl/ammos/mpsa/aerie/merlinsdk/spice/JNISpice.dll";
            } else if (osName.startsWith("linux")) {
                resourcePath = "/gov/nasa/jpl/ammos/mpsa/aerie/merlinsdk/spice/libJNISpice.so";
            } else if (osName.startsWith("mac")) {
                resourcePath = "/gov/nasa/jpl/ammos/mpsa/aerie/merlinsdk/spice/libJNISpice.jnilib";
            } else {
                throw new UnsupportedOperationException("Platform " + osName + " is not supported by JNISpice.");
            }
        }

        return SpiceLoader.class.getResource(resourcePath);
    }

    private static void deleteFileOnExit(final Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.delete(path);
            } catch (final IOException ex) {
            }
        }));
    }
}
