package gov.nasa.jpl.ammos.mpsa.aerie.time;

import gov.nasa.jpl.ammos.mpsa.aerie.utilities.IntervalAlgebra;
import gov.nasa.jpl.ammos.mpsa.aerie.utilities.IntervalSet;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class Windows implements Iterable<Window> {
  private final IntervalSet<WindowAlgebra, Window> windows = new IntervalSet<>(new WindowAlgebra());

  public Windows() {}

  public Windows(final Windows other) {
    this.windows.addAll(other.windows);
  }

  public Windows(final List<Window> windows) {
    for (final var window : windows) this.add(window);
  }

  public Windows(final Window... windows) {
    for (final var window : windows) this.add(window);
  }


  public void add(final Window window) {
    this.windows.add(window);
  }

  public void addAll(final Windows other) {
    this.windows.addAll(other.windows);
  }

  public void add(final Duration start, final Duration end) {
    this.add(Window.between(start, end));
  }

  public void add(final long start, final long end, final Duration unit) {
    this.add(Window.between(start, end, unit));
  }

  public void addPoint(final long quantity, final Duration unit) {
    this.add(Window.at(quantity, unit));
  }

  public static Windows union(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.addAll(right);
    return result;
  }


  public void subtract(final Window window) {
    this.windows.subtract(window);
  }

  public void subtractAll(final Windows other) {
    this.windows.subtractAll(other.windows);
  }

  public void subtract(final Duration start, final Duration end) {
    this.subtract(Window.between(start, end));
  }

  public void subtract(final long start, final long end, final Duration unit) {
    this.subtract(Window.between(start, end, unit));
  }

  public void subtractPoint(final long quantity, final Duration unit) {
    this.subtract(Window.at(quantity, unit));
  }

  public static Windows minus(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.subtractAll(right);
    return result;
  }

  public void intersectWith(final Window window) {
    this.windows.intersectWith(window);
  }

  public void intersectWith(final Windows other) {
    this.windows.intersectWithAll(other.windows);
  }

  public void intersectWith(final Duration start, final Duration end) {
    this.intersectWith(Window.between(start, end));
  }

  public void intersectWith(final long start, final long end, final Duration unit) {
    this.intersectWith(Window.between(start, end, unit));
  }

  public static Windows intersection(final Windows left, final Windows right) {
    final var result = new Windows(left);
    result.intersectWith(right);
    return result;
  }


  public boolean isEmpty() {
    return this.windows.isEmpty();
  }


  public boolean includes(final Window probe) {
    return this.windows.includes(probe);
  }

  public boolean includes(final Windows other) {
    return this.windows.includesAll(other.windows);
  }

  public boolean includes(final long start, final long end, final Duration unit) {
    return this.includes(Window.between(start, end, unit));
  }

  public boolean includesPoint(final long quantity, final Duration unit) {
    return this.includes(Window.at(quantity, unit));
  }


  @Override
  public Iterator<Window> iterator() {
    return this.windows.ascendingOrder().iterator();
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Windows)) return false;
    final var other = (Windows) obj;

    return Objects.equals(this.windows, other.windows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.windows);
  }

  @Override
  public String toString() {
    return this.windows.toString();
  }

  private static class WindowAlgebra implements IntervalAlgebra<WindowAlgebra, Window> {
    @Override
    public boolean isEmpty(Window x) {
      return x.isEmpty();
    }

    @Override
    public Window unify(final Window x, final Window y) {
      return Window.leastUpperBound(x, y);
    }

    @Override
    public Window intersect(final Window x, final Window y) {
      return Window.greatestLowerBound(x, y);
    }

    @Override
    public Window lowerBoundsOf(final Window x) {
      return Window.between(
          Duration.MIN_VALUE,
          (x.isEmpty()) ? Duration.MAX_VALUE : x.start.minus(Duration.EPSILON));
    }

    @Override
    public Window upperBoundsOf(final Window x) {
      return Window.between(
          (x.isEmpty()) ? Duration.MIN_VALUE : x.end.plus(Duration.EPSILON),
          Duration.MAX_VALUE);
    }

    @Override
    public Relation relationBetween(final Window x, final Window y) {
      /*
        y -----|-----  **************
        x  |           * Before
               |       * Equals
                   |   * After
           [   ]       * Contains
           [       ]   * Contains
               [   ]   * Contains

        y ---[---]---  **************
        x  |           * Before
             [   ]     * Equals
                   |   * After
             |         * ContainedBy
             [ ]       * ContainedBy
               |       * ContainedBy
               [ ]     * ContainedBy
                 |     * ContainedBy
           [ ]         * LeftOverhang
           [   ]       * LeftOverhang
           [     ]     * Contains
           [       ]   * Contains
             [     ]   * Contains
               [   ]   * RightOverhang
                 [ ]   * RightOverhang
                       **************
      */

      if (x.isEmpty()) return (y.isEmpty()) ? Relation.Equals : Relation.ContainedBy;
      if (y.isEmpty()) return Relation.Contains;

      if (y.start.compareTo(x.start) == 0 && x.end.compareTo(y.end) == 0) return Relation.Equals;
      if (x.end.compareTo(y.start) < 0) return Relation.Before;
      if (y.end.compareTo(x.start) < 0) return Relation.After;

      if (y.start.compareTo(x.start) <= 0 && x.end.compareTo(y.end) <= 0) return Relation.ContainedBy;
      if (x.end.compareTo(y.end) < 0) return Relation.LeftOverhang;
      if (y.start.compareTo(x.start) < 0) return Relation.RightOverhang;

      return Relation.Contains;
    }
  }
}
