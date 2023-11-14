package gov.nasa.jpl.aerie.contrib.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LmstSimpleTest {
  @Test
  void sanityCheck() {
    assertEquals(
      new LmstSimple.LMST(LmstSimple.SecondsStyle.MARS, 1000, 0, 0, 0, 0).toEarthMicros(),
    new LmstSimple.LMST(LmstSimple.SecondsStyle.EARTH, 1000, 0, 0, 0, 0).toEarthMicros());

    assertEquals(
        new LmstSimple.LMST(LmstSimple.SecondsStyle.MARS, 1000, 24, 0, 0, 0).toEarthMicros(),
        new LmstSimple.LMST(LmstSimple.SecondsStyle.MARS, 1001, 0, 0, 0, 0).toEarthMicros());

    assertEquals(
        new LmstSimple.LMST(LmstSimple.SecondsStyle.EARTH, 1000, 24, 39, 35, 244000).toEarthMicros(),
        new LmstSimple.LMST(LmstSimple.SecondsStyle.EARTH, 1001, 0, 0, 0, 0).toEarthMicros());

    assertTrue(
        Math.abs(new LmstSimple.LMST(LmstSimple.SecondsStyle.MARS, 1000, 1, 2, 3, 4).toEarthMicros() -
        LmstSimple.LMST.ofEarthMicros(
            LmstSimple.SecondsStyle.MARS,
            new LmstSimple.LMST(LmstSimple.SecondsStyle.MARS, 1000, 1, 2, 3, 4).toEarthMicros()
        ).toEarthMicros()) <= 1); // no more than 1 microsecond rounding error

    assertTrue(
        Math.abs(new LmstSimple.LMST(LmstSimple.SecondsStyle.EARTH, 1000, 1, 2, 3, 4).toEarthMicros() -
                 LmstSimple.LMST.ofEarthMicros(
                     LmstSimple.SecondsStyle.EARTH,
                     new LmstSimple.LMST(LmstSimple.SecondsStyle.EARTH, 1000, 1, 2, 3, 4).toEarthMicros()
                 ).toEarthMicros()) <= 1); // no more than 1 microsecond rounding error
  }

}
