package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.utilities.IntervalAlgebra;
import gov.nasa.jpl.aerie.utilities.IntervalSet;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.*;

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
    public final boolean isEmpty(Window x) {
      return x.isEmpty();
    }

    @Override
    public final Window unify(final Window x, final Window y) {
      final Duration start;
      final Inclusivity startInclusivity;

      if (x.start.shorterThan(y.start)) {
        start = x.start;
        startInclusivity = x.startInclusivity;
      } else if (y.start.shorterThan(x.start)) {
        start = y.start;
        startInclusivity = y.startInclusivity;
      } else {
        start = x.start;
        startInclusivity = (x.includesStart() || y.includesStart()) ? Inclusive : Exclusive;
      }

      final Duration end;
      final Inclusivity endInclusivity;
      if (x.end.longerThan(y.end)) {
        end = x.end;
        endInclusivity = x.endInclusivity;
      } else if (y.end.longerThan(x.end)) {
        end = y.end;
        endInclusivity = y.endInclusivity;
      } else {
        end = x.end;
        endInclusivity = (x.includesEnd() || y.includesEnd()) ? Inclusive : Exclusive;
      }

      return Window.between(start, startInclusivity, end, endInclusivity);
    }

    @Override
    public final Window intersect(final Window x, final Window y) {
      final Duration start;
      final Inclusivity startInclusivity;

      if (x.start.longerThan(y.start)) {
        start = x.start;
        startInclusivity = x.startInclusivity;
      } else if (y.start.longerThan(x.start)) {
        start = y.start;
        startInclusivity = y.startInclusivity;
      } else {
        start = x.start;
        startInclusivity = (x.includesStart() && y.includesStart()) ? Inclusive : Exclusive;
      }

      final Duration end;
      final Inclusivity endInclusivity;
      if (x.end.shorterThan(y.end)) {
        end = x.end;
        endInclusivity = x.endInclusivity;
      } else if (y.end.shorterThan(x.end)) {
        end = y.end;
        endInclusivity = y.endInclusivity;
      } else {
        end = x.end;
        endInclusivity = (x.includesEnd() && y.includesEnd()) ? Inclusive : Exclusive;
      }

      return Window.between(start, startInclusivity, end, endInclusivity);
    }

    @Override
    public final Window lowerBoundsOf(final Window x) {
      if (x.isEmpty()) return Window.FOREVER;
      return Window.between(
          Duration.MIN_VALUE,
          Inclusive, x.start,
          x.startInclusivity.opposite()
      );
    }

    @Override
    public final Window upperBoundsOf(final Window x) {
      if (x.isEmpty()) return Window.FOREVER;
      return Window.between(
          x.end,
          x.endInclusivity.opposite(), Duration.MAX_VALUE,
          Inclusive
      );
    }

    @Override
    public final Relation relationBetween(final Window x, final Window y) {
      /*
        y -----|-----  **************
        x      |       * Equals
           |           * Before
           [   )       * Meets
               (   ]   * MetBy
                   |   * After
           [   ]       * Contains
           [       ]   * Contains
               [   ]   * Contains
        y ---[---]---  **************
        x    [   ]     * Equals
           |           * Before
           [ )         * Meets
             |         * ContainedBy
             [ ]       * ContainedBy
               |       * ContainedBy
               [ ]     * ContainedBy
                 |     * ContainedBy
                 ( ]   * MetBy
                   |   * After
           [ ]         * LeftOverhang
           [   ]       * LeftOverhang
           [     ]     * Contains
           [       ]   * Contains
             [     ]   * Contains
               [   ]   * RightOverhang
                 [ ]   * RightOverhang
        y ---(---)---  **************
        x    (   )     * Equals
           |           * Before
           [ )         * Before
           [ ]         * Meets
             ( ]       * ContainedBy
               |       * ContainedBy
               [ )     * ContainedBy
                 [ ]   * MetBy
                 ( ]   * After
                   |   * After
           [   ]       * LeftOverhang
           [     ]     * Contains
           [       ]   * Contains
             [     ]   * Contains
               [   ]   * RightOverhang
                       **************
      */

      if (compareStartToStart(x, y) == 0 && compareEndToEnd(x, y) == 0) return Relation.Equals;
      if (compareStartToStart(x, y) <= 0 && compareEndToEnd(y, x) <= 0) return Relation.Contains;
      if (compareStartToStart(y, x) <= 0 && compareEndToEnd(x, y) <= 0) return Relation.ContainedBy;

      if (x.end.isEqualTo(y.start) && y.includesStart() != x.includesEnd()) return Relation.Meets;
      if (y.end.isEqualTo(x.start) && x.includesStart() != y.includesEnd()) return Relation.MetBy;

      if (x.end.isEqualTo(y.start) && y.includesStart()) return Relation.LeftOverhang;
      if (y.end.isEqualTo(x.start) && x.includesStart()) return Relation.RightOverhang;

      if (x.end.noLongerThan(y.start)) return Relation.Before;
      if (y.end.noLongerThan(x.start)) return Relation.After;

      if (x.start.shorterThan(y.start)) return Relation.LeftOverhang;
      else return Relation.RightOverhang;
    }

    private int compareStartToStart(final Window x, final Window y) {
      // First, order by absolute time.
      if (!x.start.isEqualTo(y.start)) {
        return x.start.compareTo(y.start);
      }

      // Second, order by whichever one includes the point.
      if (x.includesStart() != y.includesStart()) {
        return (x.includesStart()) ? -1 : 1;
      }

      return 0;
    }

    private int compareEndToEnd(final Window x, final Window y) {
      // First, order by absolute time.
      if (!x.end.isEqualTo(y.end)) {
        return x.end.compareTo(y.end);
      }

      // Second, order by whichever one includes the point
      if (x.includesEnd() != y.includesEnd()) {
        return (x.includesEnd()) ? -1 : 1;
      }

      return 0;
    }
  }
}
