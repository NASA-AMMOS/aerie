package gov.nasa.jpl.aerie.constraints;

import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Utilities {

  public static void assertEquivalent(final LinearProfile expected, final LinearProfile actual) {
    assertEquals(expected, actual);

    assertTrue(areEquivalent(expected.profilePieces, actual.profilePieces));
  }

  public static void assertEquivalent(final Windows expected, final Windows actual) {
    assertEquals(expected, actual);

    // Things that are equal ought to be observationally equivalent.
    assertTrue(areEquivalent(expected, actual));
  }

  public static void assertEquivalent(final Collection<Window> expected, final Windows actual) {
    assertTrue(areEquivalent(expected.iterator(), actual.iterator()));
  }

  private static <T> boolean areEquivalent(final Iterable<T> xs, final Iterable<T> ys) {
    return areEquivalent(xs.iterator(), ys.iterator());
  }

  private static <T> boolean areEquivalent(final Iterator<T> xsIter, final Iterator<T> ysIter) {
    while (true) {
      if (!xsIter.hasNext()) return !ysIter.hasNext();
      if (!ysIter.hasNext()) return false;

      final var x = xsIter.next();
      final var y = ysIter.next();
      if (!Objects.equals(x, y)) return false;
    }
  }
}
