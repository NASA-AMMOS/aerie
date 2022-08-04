package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.constraints.UnsplittableIntervalException;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;

/**
 * A collection of intervals that can overlap.
 */
public class Spans implements IntervalContainer<Spans>, Iterable<Interval> {
  private final List<Interval> intervals;

  public Spans() {
    this.intervals = new ArrayList<>();
  }

  public Spans(final ArrayList<Interval> intervals) {
    intervals.removeIf(Interval::isEmpty);
    this.intervals = intervals;
  }

  public Spans(final Iterable<Interval> iter) {
    this.intervals = StreamSupport.stream(iter.spliterator(), false).filter($ -> !$.isEmpty()).toList();
  }

  public Spans(final Interval... intervals) {
    this.intervals = Arrays.stream(intervals).filter($ -> !$.isEmpty()).toList();
  }

  public Windows intoWindows() {
    return new Windows(false).set(this.intervals, true);
  }

  public void add(final Interval window) {
    if (!window.isEmpty()) {
      this.intervals.add(window);
    }
  }

  public void addAll(final Iterable<Interval> iter) {
    this.intervals.addAll(
        StreamSupport
            .stream(iter.spliterator(), false)
            .filter($ -> !$.isEmpty())
            .toList()
    );
  }

  public Spans map(final Function<Interval, Interval> mapper) {
    return new Spans(this.intervals.stream().map(mapper).filter($ -> !$.isEmpty()).toList());
  }

  public Spans flatMap(final Function<Interval, ? extends Stream<Interval>> mapper) {
    return new Spans(this.intervals.stream().flatMap(mapper).filter($ -> !$.isEmpty()).toList());
  }

  public Spans filter(final Predicate<Interval> filter) {
    return new Spans(this.intervals.stream().filter(filter).toList());
  }

  @Override
  public Spans split(final int numberOfSubSpans) {
    if (numberOfSubSpans == 1) {
      return new Spans(this);
    }
    return this.flatMap(x -> {
      // Width of each sub-window, rounded down to the microsecond
      final var width = Duration.divide(Duration.subtract(x.end, x.start), numberOfSubSpans);

      final var numberOfMicroSeconds = Duration.subtract(x.end, x.start).in(Duration.MICROSECOND);

      // We throw an exception if the interval contains fewer microseconds than the requested number of sub-spans.
      if (x.isSingleton()) {
        throw new UnsplittableIntervalException("Cannot split an instantaneous interval into " + numberOfSubSpans + " pieces.");
      } else if (numberOfMicroSeconds < numberOfSubSpans) {
        throw new UnsplittableIntervalException("Cannot split an interval only " + numberOfMicroSeconds + " microseconds long into " + numberOfSubSpans + " pieces.");
      }

      var cursor = Duration.add(x.start, width);
      final List<Window> ret = new ArrayList<>();
      ret.add(Window.between(x.start, x.startInclusivity, cursor, Exclusive));
      for (int i = 1; i < numberOfSubSpans - 1; i++) {
        final var nextCursor = Duration.add(cursor, width);
        ret.add(Window.between(cursor, Exclusive, nextCursor, Exclusive));
        cursor = nextCursor;
      }
      ret.add(Window.between(cursor, Exclusive, x.end, x.endInclusivity));
      return ret.stream();
    });
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof final Spans spans)) return false;

    return Objects.equals(this.intervals, spans.intervals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.intervals);
  }

  @Override
  public String toString() {
    return this.intervals.toString();
  }

  @Override
  public Iterator<Interval> iterator() {
    return this.intervals.iterator();
  }
}
