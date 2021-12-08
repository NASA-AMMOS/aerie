package gov.nasa.jpl.aerie.banananation;

import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;

public final class BananaValueMappers {
  public static ValueMapper<Configuration> configuration() {
    return new ConfigurationValueMapper();
  }

  public static <T extends Comparable<T>> ValueMapper<BananaRange<T>> bananaRange(final ValueMapper<T> elementMapper) {
    return new BananaRangeValueMapper<>(elementMapper);
  }
}
