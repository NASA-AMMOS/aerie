package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Predicate;

public final class AdaptationLoader {
    public static MerlinAdaptation loadAdaptation(final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        return loadAdaptationProvider(path, name, version).get();
    }

    public static Provider<MerlinAdaptation> loadAdaptationProvider(final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        Objects.requireNonNull(path);

        final URL adaptationURL;
        try {
            // Construct a ClassLoader with access to classes in the adaptation location.
            adaptationURL = path.toUri().toURL();
        } catch (final MalformedURLException ex) {
            // This exception only happens if there is no URL protocol handler available to represent a Path.
            // This is highly unexpected, and indicates a fundamental problem with the system environment.
            throw new Error(ex);
        }

        final ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader classLoader = new URLClassLoader(new URL[]{adaptationURL}, parentClassLoader);

        // Look for MerlinAdaptation implementors in the adaptation.
        final ServiceLoader<MerlinAdaptation> serviceLoader = ServiceLoader.load(MerlinAdaptation.class, classLoader);

        // Return the first adaptation matching the given metadata. (For correctness, we're assuming there's only one
        // matching MerlinAdaptation in any given adaptation.)
        return serviceLoader
            .stream()
            .filter(byMetadata(name, version))
            .findFirst()
            .orElseThrow(() -> new AdaptationLoadException("No implementation found for `" + MerlinAdaptation.class.getSimpleName() + "`"));
    }

    private static Predicate<Provider<MerlinAdaptation>> byMetadata(final String name, final String version) {
        return (provider) -> {
            final var metadata = provider.type().getAnnotation(Adaptation.class);
            return Objects.equals(name, metadata.name()) && Objects.equals(version, metadata.version());
        };
    }

    public static class AdaptationLoadException extends Exception {
        public AdaptationLoadException(final String message) {
            super(message);
        }
    }
}
