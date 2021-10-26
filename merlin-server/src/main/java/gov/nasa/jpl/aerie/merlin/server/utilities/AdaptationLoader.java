package gov.nasa.jpl.aerie.merlin.server.utilities;

import gov.nasa.jpl.aerie.merlin.driver.Adaptation;
import gov.nasa.jpl.aerie.merlin.driver.AdaptationBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.model.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.MerlinPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class AdaptationLoader {
    public static AdaptationFactory<?> loadAdaptationFactory(final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        final var service = loadAdaptationProvider(path, name, version);
        return service.getFactory();
    }

    public static Adaptation<?, ?> loadAdaptation(final SerializedValue missionModelConfig, final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        final var service = loadAdaptationProvider(path, name, version);
        final var factory = service.getFactory();
        final var builder = new AdaptationBuilder<>();
        return loadAdaptation(missionModelConfig, factory, builder);
    }

    private static <$Schema, Model>
    Adaptation<$Schema, Model> loadAdaptation(
        final SerializedValue missionModelConfig,
        final AdaptationFactory<Model> factory,
        final AdaptationBuilder<$Schema> builder
    ) {
        final var model = factory.instantiate(missionModelConfig, builder);
        return builder.build(model, factory.getTaskSpecTypes());
    }

    public static MerlinPlugin loadAdaptationProvider(final Path path, final String name, final String version)
        throws AdaptationLoadException
    {
        // Look for a MerlinAdaptation implementor in the adaptation. For correctness, we're assuming there's
        // only one matching MerlinAdaptation in any given adaptation.
        final var className = getImplementingClassName(path, name, version);

        // Construct a ClassLoader with access to classes in the adaptation location.
        final var parentClassLoader = Thread.currentThread().getContextClassLoader();
        final var classLoader = new URLClassLoader(new URL[] {adaptationPathToUrl(path)}, parentClassLoader);

        try {
            final var factoryClass$ = classLoader.loadClass(className);
            if (!MerlinPlugin.class.isAssignableFrom(factoryClass$)) {
                throw new AdaptationLoadException(path, name, version);
            }

            // SAFETY: We checked above that AdaptationFactory is assignable from this type.
            @SuppressWarnings("unchecked")
            final var factoryClass = (Class<? extends MerlinPlugin>) factoryClass$;

            return factoryClass.getConstructor().newInstance();
        } catch (final ClassNotFoundException | NoSuchMethodException | InstantiationException
            | IllegalAccessException | InvocationTargetException ex)
        {
            throw new AdaptationLoadException(path, name, version, ex);
        }
    }

    private static String getImplementingClassName(final Path jarPath, final String name, final String version)
    throws AdaptationLoadException {
        try {
            final var jarFile = new JarFile(jarPath.toFile());
            final var jarEntry = jarFile.getEntry("META-INF/services/" + MerlinPlugin.class.getCanonicalName());
            final var inputStream = jarFile.getInputStream(jarEntry);

            final var classPathList = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.toList());

            if (classPathList.size() != 1) {
                throw new AdaptationLoadException(jarPath, name, version);
            }

            return classPathList.get(0);
        } catch (final IOException ex) {
            throw new AdaptationLoadException(jarPath, name, version, ex);
        }
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
            this(path, name, version, null);
        }

        private AdaptationLoadException(final Path path, final String name, final String version, final Throwable cause) {
            super(
                String.format(
                    "No implementation found for `%s` at path `%s` wih name \"%s\" and version \"%s\"",
                    MerlinPlugin.class.getSimpleName(),
                    path,
                    name,
                    version),
                cause);
        }
    }
}
