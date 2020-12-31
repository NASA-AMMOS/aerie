package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ByteValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.CharacterValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DurationValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.EnumValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.FloatValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ListValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.MapValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveBooleanArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveByteArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveCharArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveDoubleArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveFloatArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveIntArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveLongArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveShortArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.StringValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.time.Duration;

import java.util.List;
import java.util.Map;

public final class BasicValueMappers {
  public static ValueMapper<Boolean> $boolean() {
    return new BooleanValueMapper();
  }

  public static ValueMapper<Byte> $byte() {
    return new ByteValueMapper();
  }

  public static ValueMapper<Short> $short() {
    return new ShortValueMapper();
  }

  public static ValueMapper<Integer> $int() {
    return new IntegerValueMapper();
  }

  public static ValueMapper<Long> $long() {
    return new LongValueMapper();
  }

  public static ValueMapper<Character> $char() {
    return new CharacterValueMapper();
  }

  public static ValueMapper<Float> $float() {
    return new FloatValueMapper();
  }

  public static ValueMapper<Double> $double() {
    return new DoubleValueMapper();
  }


  public static ValueMapper<byte[]> byteArray() {
    return new PrimitiveByteArrayValueMapper();
  }

  public static ValueMapper<short[]> shortArray() {
    return new PrimitiveShortArrayValueMapper();
  }

  public static ValueMapper<int[]> intArray() {
    return new PrimitiveIntArrayValueMapper();
  }

  public static ValueMapper<long[]> longArray() {
    return new PrimitiveLongArrayValueMapper();
  }

  public static ValueMapper<float[]> floatArray() {
    return new PrimitiveFloatArrayValueMapper();
  }

  public static ValueMapper<double[]> doubleArray() {
    return new PrimitiveDoubleArrayValueMapper();
  }

  public static ValueMapper<char[]> charArray() {
    return new PrimitiveCharArrayValueMapper();
  }

  public static ValueMapper<boolean[]> booleanArray() {
    return new PrimitiveBooleanArrayValueMapper();
  }


  public static <T> ValueMapper<T[]> array(final Class<T> elementClass, final ValueMapper<T> elementMapper) {
    return new ArrayValueMapper<>(elementMapper, elementClass);
  }

  public static <E extends Enum<E>> ValueMapper<E> $enum(final Class<E> enumClass) {
    return new EnumValueMapper<>(enumClass);
  }


  public static ValueMapper<String> string() {
    return new StringValueMapper();
  }

  public static <T> ValueMapper<List<T>> list(final ValueMapper<T> elementMapper) {
    return new ListValueMapper<>(elementMapper);
  }

  public static <K, V> ValueMapper<Map<K, V>> map(final ValueMapper<K> keyMapper, final ValueMapper<V> valueMapper) {
    return new MapValueMapper<>(keyMapper, valueMapper);
  }

  public static ValueMapper<Duration> duration() {
    return new DurationValueMapper();
  }
}
