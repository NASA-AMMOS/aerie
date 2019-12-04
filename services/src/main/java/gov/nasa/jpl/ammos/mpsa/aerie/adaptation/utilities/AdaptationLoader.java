package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.utilities;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.exceptions.AdaptationContractException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public final class AdaptationLoader {
    public static Map<String, ActivityType> loadActivities(final Path path) throws AdaptationContractException {
        final MerlinAdaptation adaptation = loadAdaptation(path);

        final Map<String, Map<String, ParameterSchema>> activitySchemas = Optional
            .of(adaptation)
            .map(MerlinAdaptation::getActivityMapper)
            .map(ActivityMapper::getActivitySchemas)
            .orElseGet(HashMap::new);

        return activitySchemas
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                p -> p.getKey(),
                p -> new ActivityType(p.getKey(), p.getValue())));
    }

    public static MerlinAdaptation<?> loadAdaptation(final Path adaptationPath) throws AdaptationContractException {
      Objects.requireNonNull(adaptationPath);

      final URL adaptationURL;
      try {
          // Construct a ClassLoader with access to classes in the adaptation location.
          adaptationURL = adaptationPath.toUri().toURL();
      } catch (final MalformedURLException ex) {
          // This exception only happens if there is no URL protocol handler available to represent a Path.
          // This is highly unexpected, and indicates a fundamental problem with the system environment.
          throw new Error(ex);
      }

      final ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
      final ClassLoader classLoader = new URLClassLoader(new URL[]{adaptationURL}, parentClassLoader);

      // Look for MerlinAdaptation implementors in the adaptation.
      final ServiceLoader<MerlinAdaptation> serviceLoader =
          ServiceLoader.load(MerlinAdaptation.class, classLoader);

      // Return the first we come across. (This may not be deterministic, so for correctness
      // we're assuming there's only one MerlinAdaptation in any given location.
      return serviceLoader
          .findFirst()
          .orElseThrow(() -> new AdaptationContractException("No implementation found for `" + MerlinAdaptation.class.getSimpleName() + "`"));
  }
}
