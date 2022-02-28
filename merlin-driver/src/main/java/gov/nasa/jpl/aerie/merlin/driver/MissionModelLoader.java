package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.MerlinPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
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

public final class MissionModelLoader {
    public static MissionModelFactory<?> loadMissionModelFactory(final Path path, final String name, final String version)
        throws MissionModelLoadException
    {
        final var service = loadMissionModelProvider(path, name, version);
        return service.getFactory();
    }

    public static MissionModel<?> loadMissionModel(final SerializedValue missionModelConfig, final Path path, final String name, final String version)
        throws MissionModelLoadException
    {
        final var service = loadMissionModelProvider(path, name, version);
        final var factory = service.getFactory();
        final var builder = new MissionModelBuilder();
        return loadMissionModel(missionModelConfig, factory, builder);
    }

    public static <Model>
    MissionModel<Model> loadMissionModel(
        final SerializedValue missionModelConfig,
        final MissionModelFactory<Model> factory,
        final MissionModelBuilder builder
    ) {
        final var model = factory.instantiate(missionModelConfig, builder);
        return builder.build(model, factory.getConfigurationType(), factory.getTaskSpecTypes());
    }

    public static MerlinPlugin loadMissionModelProvider(final Path path, final String name, final String version)
        throws MissionModelLoadException
    {
        // Look for a MerlinMissionModel implementor in the mission model. For correctness, we're assuming there's
        // only one matching MerlinMissionModel in any given mission model.
        final var className = getImplementingClassName(path, name, version);

        // Construct a ClassLoader with access to classes in the mission model location.
        final var parentClassLoader = Thread.currentThread().getContextClassLoader();
        final var classLoader = new URLClassLoader(new URL[] {missionModelPathToUrl(path)}, parentClassLoader);

        try {
            final var factoryClass$ = classLoader.loadClass(className);
            if (!MerlinPlugin.class.isAssignableFrom(factoryClass$)) {
                throw new MissionModelLoadException(path, name, version);
            }

            // SAFETY: We checked above that MissionModelFactory is assignable from this type.
            @SuppressWarnings("unchecked")
            final var factoryClass = (Class<? extends MerlinPlugin>) factoryClass$;

            return factoryClass.getConstructor().newInstance();
        } catch (final ClassNotFoundException | NoSuchMethodException | InstantiationException
            | IllegalAccessException | InvocationTargetException ex)
        {
            throw new MissionModelLoadException(path, name, version, ex);
        }
    }

    private static String getImplementingClassName(final Path jarPath, final String name, final String version)
    throws MissionModelLoadException {
        try {
            final var jarFile = new JarFile(jarPath.toFile());
            final var jarEntry = jarFile.getEntry("META-INF/services/" + MerlinPlugin.class.getCanonicalName());
            final var inputStream = jarFile.getInputStream(jarEntry);

            final var classPathList = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .toList();

            if (classPathList.size() != 1) {
                throw new MissionModelLoadException(jarPath, name, version);
            }

            return classPathList.get(0);
        } catch (final IOException ex) {
            throw new MissionModelLoadException(jarPath, name, version, ex);
        }
    }

    private static URL missionModelPathToUrl(final Path path) {
        try {
            return path.toUri().toURL();
        } catch (final MalformedURLException ex) {
            // This exception only happens if there is no URL protocol handler available to represent a Path.
            // This is highly unexpected, and indicates a fundamental problem with the system environment.
            throw new Error(ex);
        }
    }

    public static class MissionModelLoadException extends Exception {
        private MissionModelLoadException(final Path path, final String name, final String version) {
            this(path, name, version, null);
        }

        private MissionModelLoadException(final Path path, final String name, final String version, final Throwable cause) {
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
