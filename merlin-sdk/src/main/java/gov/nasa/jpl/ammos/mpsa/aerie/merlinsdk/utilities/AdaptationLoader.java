package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;

import java.util.Map;
import java.util.ServiceLoader;
import static java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

public final class AdaptationLoader {
  private AdaptationLoader() {}

  public static Map<Adaptation, Provider<MerlinAdaptation>> listAdaptations(final ClassLoader classLoader) {
    return ServiceLoader
        .load(MerlinAdaptation.class, classLoader)
        .stream()
        .collect(Collectors.toMap(p -> p.type().getAnnotation(Adaptation.class), p -> p));
  }
}