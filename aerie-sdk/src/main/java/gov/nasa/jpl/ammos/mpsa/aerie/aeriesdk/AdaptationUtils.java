package gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;

public class AdaptationUtils {
    // TODO: Move this into the Adaptation Runtime Service when it is complete
    // TODO: Allow lookup by adaptation metadata (e.g. name and version).
    public static MerlinAdaptation loadAdaptation(String adaptationLocation) throws IOException {
        // Construct a ClassLoader with access to classes in the adaptation location.
        final URL adaptationURL = new File(adaptationLocation).toURI().toURL();
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
