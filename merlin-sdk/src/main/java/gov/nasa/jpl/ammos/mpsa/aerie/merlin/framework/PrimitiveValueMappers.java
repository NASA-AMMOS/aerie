package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.BooleanValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ByteValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.CharacterValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.DoubleValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.FloatValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.IntegerValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.LongValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ShortValueMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

public final class PrimitiveValueMappers {
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
}
