package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveBooleanArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveByteArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveCharArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveDoubleArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveFloatArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveIntArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveLongArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.PrimitiveShortArrayValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

public final class PrimitiveArrayValueMappers {
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
}
