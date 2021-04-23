package gov.nasa.jpl.aerie.merlin.server.utilities;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

public final class AdaptationLoader {
    public static Adaptation<?> loadAdaptation(final SerializedValue missionModelConfig, final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        final var factory = loadAdaptationProvider(path, name, version).get();
        final var builder = new AdaptationBuilder<>(Schema.builder());
        factory.instantiate(missionModelConfig, builder);
        return builder.build();
    }

    public static Provider<AdaptationFactory> loadAdaptationProvider(final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        // Construct a ClassLoader with access to classes in the adaptation location.
        final var parentClassLoader = Thread.currentThread().getContextClassLoader();
        final var classLoader = new URLClassLoader(new URL[] {adaptationPathToUrl(path)}, parentClassLoader);

        // Look for MerlinAdaptation implementor in the adaptation. For correctness, we're assuming there's
        // only one matching MerlinAdaptation in any given adaptation.
        return ServiceLoader
            .load(AdaptationFactory.class, classLoader)
            .stream()
            .findFirst()
            .orElseThrow(() -> new AdaptationLoadException(path, name, version));
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
                AdaptationFactory.class.getSimpleName(),
                path,
                name,
                version));
        }
    }
}
