package gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ServiceLoader;

public class AdaptationUtils {
    // TODO: Move this into the Adaptation Runtime Service when it is complete
    // TODO: Allow lookup by adaptation metadata (e.g. name and version).
    // TODO: This should throw an additional exception when a valid JAR does not contain a valid adaptation
    public static MerlinAdaptation loadAdaptation(final Path adaptationPath) throws IOException {
        // Construct a ClassLoader with access to classes in the adaptation location.
        final URL adaptationURL = adaptationPath.toUri().toURL();
        final ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader classLoader = new URLClassLoader(new URL[]{adaptationURL}, parentClassLoader);

        // Look for MerlinAdaptation implementors in the adaptation.
        final ServiceLoader<MerlinAdaptation> serviceLoader =
            ServiceLoader.load(MerlinAdaptation.class, classLoader);

        // Return the first we come across. (This may not be deterministic, so for correctness
        // we're assuming there's only one MerlinAdaptation in any given location.
        return serviceLoader.findFirst().orElse(null);
    }
}
