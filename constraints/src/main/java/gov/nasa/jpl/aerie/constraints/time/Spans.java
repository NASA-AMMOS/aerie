package gov.nasa.jpl.aerie.constraints.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection of intervals that can overlap.
 */
public class Spans implements Iterable<Interval> {
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
    return Windows.definedEverywhere(this.intervals, true);
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
