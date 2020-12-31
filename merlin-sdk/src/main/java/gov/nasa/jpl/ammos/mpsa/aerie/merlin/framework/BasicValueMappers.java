package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DurationValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ListValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.MapValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.util.List;
import java.util.Map;

public final class BasicValueMappers {
  public static ValueMapper<String> string() {
    return new StringValueMapper();
  }

  public static ValueMapper<Duration> duration() {
    return new DurationValueMapper();
  }

  public static <E extends Enum<E>> ValueMapper<E> $enum(final Class<E> enumClass) {
    return new EnumValueMapper<>(enumClass);
  }

  public static <T> ValueMapper<T[]> array(final Class<T> elementClass, final ValueMapper<T> elementMapper) {
    return new ArrayValueMapper<>(elementMapper, elementClass);
  }

  public static <T> ValueMapper<List<T>> list(final ValueMapper<T> elementMapper) {
    return new ListValueMapper<>(elementMapper);
  }

  public static <K, V> ValueMapper<Map<K, V>> map(final ValueMapper<K> keyMapper, final ValueMapper<V> valueMapper) {
    return new MapValueMapper<>(keyMapper, valueMapper);
  }
}
