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
    // The Class object representing the MerlinAdaptation interface type.
    // Because `X.class` and `x.getClass()` always give the raw type, we have to manually add a wildcard to the type.
    // Generics get erased during compilation anyway, so this should not cause an error.
    @SuppressWarnings("unchecked")
    private static final Class<MerlinAdaptation<?>> adaptationClass =
        (Class<MerlinAdaptation<?>>) (Object)
            MerlinAdaptation.class;

    public static MerlinAdaptation<?> loadAdaptation(final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        return loadAdaptationProvider(path, name, version).get();
    }

    public static Provider<MerlinAdaptation<?>> loadAdaptationProvider(final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        // Construct a ClassLoader with access to classes in the adaptation location.
        final var parentClassLoader = Thread.currentThread().getContextClassLoader();
        final var classLoader = new URLClassLoader(new URL[] {adaptationPathToUrl(path)}, parentClassLoader);

        // Look for MerlinAdaptation implementors in the adaptation, and get the first one matching the given metadata.
        // (For correctness, we're assuming there's only one matching MerlinAdaptation in any given adaptation.)
        return ServiceLoader
            .load(adaptationClass, classLoader)
            .stream()
            .filter(byMetadata(name, version))
            .findFirst()
            .orElseThrow(() -> new AdaptationLoadException(path, name, version));
    }

    private static Predicate<Provider<MerlinAdaptation<?>>> byMetadata(final String name, final String version) {
        return (provider) -> {
            final var metadata = provider.type().getAnnotation(Adaptation.class);
            return Objects.equals(name, metadata.name()) && Objects.equals(version, metadata.version());
        };
    }

    private static URL adaptationPathToUrl(final Path path) {
        try {
            return path.toUri().toURL();
        } catch (final MalformedURLException ex) {
            // This exception only happens if there is no URL protocol handler available to represent a Path.
            // This is highly unexpected, and indicates a fundamental problem with the system environment.
            throw new Error(ex);
        }
    }

    public static class AdaptationLoadException extends Exception {
        private AdaptationLoadException(final Path path, final String name, final String version) {
            super(String.format(
                "No implementation found for `%s` at path `%s` wih name \"%s\" and version \"%s\"",
                MerlinAdaptation.class.getSimpleName(),
                path,
                name,
                version));
        }
    }
}
