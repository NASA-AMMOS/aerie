package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A boolean profile, which can contain gaps (a.k.a. nulls).
 *
 * Backed by an {@link IntervalMap} of type {@link Boolean}. This class provides additional operations
 * which are only valid on bools.
 */
public final class Windows implements Iterable<Segment<Boolean>> {

  private final IntervalMap<Boolean> segments;

  /** Creates an empty Windows */
  public Windows() {
    this.segments = new IntervalMap<>();
  }

  /** Creates a Windows from potentially unordered, overlapping segments */
  @SafeVarargs
  public Windows(final Segment<Boolean>... segments) {
    this.segments = new IntervalMap<>(segments);
  }

  /** Creates a Windows from potentially unordered, overlapping segments */
  public Windows(final List<Segment<Boolean>> segments) {
    this.segments = new IntervalMap<>(segments);
  }

  /** Creates a Windows with a single segment */
  public Windows(final Interval interval, final boolean value) {
    this.segments = new IntervalMap<>(interval, value);
  }

  /** Creates a Windows that is equal to a given value for all representable times */
  public Windows(final boolean bool) {
    this.segments = new IntervalMap<>(bool);
  }

  /** Wraps an IntervalMap of Booleans in Windows. */
  private Windows(final IntervalMap<Boolean> segments) {
    this.segments = segments;
  }

  /**
   * Perform the and operation on two Windows.
   *
   * The operation is symmetric, even in the case of gaps. The truth table is:
   *
   * L | R | L and R
   * :---:|:---:|:---:
   * T | T | T
   * T | F | F
   * F | F | F
   * T | N | N
   * F | N | F
   * N | N | N
   *
   * N stands for Null.
   *
   * @param other right operand
   * @return a new Windows
   */
  public Windows and(final Windows other) {
    return new Windows(IntervalMap.map2(
        this.segments, other.segments,
        (l, r) -> {
          if (l.isPresent() && r.isPresent()) {
            return Optional.of(l.get() && r.get());
          } else if (l.isPresent()) {
            return l.get() ? Optional.empty() : Optional.of(Boolean.FALSE);
          } else if (r.isPresent()) {
            return r.get() ? Optional.empty() : Optional.of(Boolean.FALSE);
          } else {
            return Optional.empty();
          }
        }
    ));
  }

  /**
   * Perform the or operation on two Windows.
   *
   * The operation is symmetric, even in the case of gaps. The truth table is:
   *
   * L | R | L or R
   * :---:|:---:|:---:
   * T | T | T
   * T | F | T
   * F | F | F
   * T | N | T
   * F | N | N
   * N | N | N
   *
   * N stands for Null.
   *
   * @param other right operand
   * @return a new Windows
   */
  public Windows or(final Windows other) {
    return new Windows(IntervalMap.map2(
        this.segments, other.segments,
        (l, r) -> {
          if (l.isPresent() && r.isPresent()) {
            return Optional.of(l.get() || r.get());
          } else if (l.isPresent()) {
            return l.get() ? Optional.of(true) : Optional.empty();
          } else if (r.isPresent()) {
            return r.get() ? Optional.of(true) : Optional.empty();
          } else {
            return Optional.empty();
          }
        }
    ));
  }

  /**
   * "Adds" two Windows together.
   *
   * It is almost identical to {@link Windows#or}, except that F + N = F instead of N.
   *
   * L | R | L + R
   * :---:|:---:|:---:
   * T | T | T
   * T | F | T
   * F | F | F
   * T | N | T
   * F | N | F
   * N | N | N
   *
   * N stands for Null.
   *
   * @param other right operand
   * @return a new Windows
   */
  public Windows add(final Windows other) {
    return new Windows(IntervalMap.map2(
        this.segments, other.segments,
        (l, r) -> {
          if (l.isPresent() && r.isPresent()) {
            return Optional.of(l.get() || r.get());
          } else if (l.isPresent()) {
            return l;
          } else return r;
        }
    ));
  }

  /**
   * Inverts the truth value of each segment.
   *
   * Leaves gaps unchanged.
   *
   * @return a new Windows
   */
  public Windows not() {
    //should not be a subtraction because then if it was null originally, then subtracting original from forever
    //  yields true where once was null, which isn't good. we want a simple inversion of true and false here, without
    //  filling nulls.
    return new Windows(this.segments.map($ -> $.map(b -> !b)));
  }

  /** Gets the time and inclusivity of the leading edge of the first true segment */
  public Optional<Pair<Duration, Interval.Inclusivity>> minTrueTimePoint(){
    for (final var segment: this.segments) {
      if (segment.value()) {
        final var window = segment.interval();
        return Optional.of(Pair.of(window.start, window.startInclusivity));
      }
    }
    return Optional.empty();
  }

  /** Gets the time and inclusivity of the trailing edge of the last true segment */
  public Optional<Pair<Duration, Interval.Inclusivity>> maxTrueTimePoint(){
    for (int i = this.segments.size() - 1; i >= 0; i--) {
      final var segment = this.segments.get(i);
      if (segment.value()) {
        final var window = segment.interval();
        return Optional.of(Pair.of(window.end, window.endInclusivity));
      }
    }
    return Optional.empty();
  }

  /**
   * Sets all true segments that are not fully contained in the interval to false.
   *
   * @return a new Windows
   */
  public Windows trueSubsetContainedIn(final Interval interval) {
    var result = new Windows(interval, false);
    for (final var segment: this.segments) {
      if (segment.value() && interval.contains(segment.interval())) {
        result = result.set(segment.interval(), true);
      }
    }
    return result;
  }

  /**
   * Sets a specific true segment to false.
   *
   * If the argument is N and N >= 0, it will set the Nth true segment to false.
   * If N < 0, it will index true segments from the end instead, where N = -1 is the last true segment.
   *
   * @return a new Windows
   */
  public Windows removeTrueSegment(final int indexToRemove) {
    if (indexToRemove >= 0) {
      int index = 0;
      for (final var interval : this.segments.iterateEqualTo(true)) {
        if (index == indexToRemove) {
          return new Windows(this.segments.set(interval, false));
        } else {
          index += 1;
        }
      }
    } else {
      int index = -1;
      for (int i = this.segments.size() - 1; i >= 0; i--) {
        final var segment = this.segments.get(i);
        if (segment.value()) {
          if (index == indexToRemove) {
            return new Windows(this.segments.set(segment.interval(), false));
          } else {
            index -= 1;
          }
        }
      }
    }
    return this;
  }

  /**
   * Sets all but a specific true segment to false.
   *
   * If the argument is N and N >= 0, it will set all but the Nth true segment to false.
   * If N < 0, it will index true segments from the end instead, where N = -1 is the last true segment.
   *
   * @return a new Windows
   */
  public Windows keepTrueSegment(final int indexToKeep) {
    var result = new Windows(this.segments.stream().toList());
    if (indexToKeep >= 0) {
      int index = 0;
      for (final var interval : this.segments.iterateEqualTo(true)) {
        if (index != indexToKeep) {
          result.segments.insertInPlace(Segment.of(interval, false), 0);
        }
        index += 1;
      }
    } else {
      int index = -1;
      for (int i = this.segments.size() - 1; i >= 0; i--) {
        final var segment = this.segments.get(i);
        if (segment.value()) {
          if (index != indexToKeep) {
            result.segments.insertInPlace(Segment.of(segment.interval(), false), 0);
          }
          index -= 1;
        }
      }
    }
    return result;
  }

  /** Whether all the true segments of the given Windows are contained in the true segments of this. */
  public boolean includes(final Windows other) {
    //if you have:
    //  other:    ---TTTT---TTT------
    //  original: ---------TTTFF-----
    //  then you fail twice, once because first interval not contained at all, second because overlap with false
    //  we can do this with a map2 with a truthtable, so wherever inclusion holds we say true, if its wrong we say false
    //  and then reduce and if there's any falses you failed overall.

    // other |  orig   | output
    //  T    |    T     |   T
    //  T    |    F     |   F
    //  T    |    N     |   F
    //  F    |    T     |   T     //probably won't pass false as a value anyways, but just in case we should handle
    //  F    |    F     |   T     //  in case user passes a NewWindows from another method that has falses...
    //  F    |    N     |   N     //since its false, not a problem if undefined. we handle actual null checks in isNotNull
    //  N    |    T     |   N
    //  N    |    F     |   N
    //  N    |    N     |   N

    final var inclusion = IntervalMap.map2(
        this.segments, other.segments,
        ($original, $other) -> $other.map($ -> !$ || ($original.isPresent() && $original.get()))
    );

    //anywhere where the above has false means inclusion wasn't perfect, so squash and get a truth value:
    return StreamSupport.stream(inclusion.spliterator(), false).allMatch(Segment::value);
  }

  /** Whether the given interval is contained in a true segment in this. */
  public boolean includes(final Interval probe) {
    return this.includes(new Windows(probe, true));
  }

  /**
   * Sets true segments shorter than `minDur` or longer than `maxDur` to false.
   *
   * @return a new Windows
   */
  public Windows filterByDuration(Duration minDur, Duration maxDur){
    if (minDur.longerThan(maxDur)) {
      throw new IllegalArgumentException("MaxDur %s must be greater than MinDur %s".formatted(minDur.toString(), maxDur.toString()));
    }

    //if you have:
    //  input:  ---TTTTFFFFTTTTFFFFTT---FFFTTTFF--TTT---, 3, 3
    //  output: ---FFFFFFFFFFFFFFFFTT---FFF
    //  then you want to shorten only the trues, and replace them with F, don't mess with nulls
    //  so if false interval encountered, keep the same. if true interval encountered, and it is not in filter, replace
    //     with false
    //  if true interval enountered, and true, keep the same
    //  if null, keep null

    // orig | inFilter | output
    //  T   |    T     |   T
    //  T   |    F     |   F
    //  F   |    T     |   F
    //  F   |    F     |   F
    //  N   |    T     |   N
    //  N   |    F     |   N


    return new Windows(this.segments.map((value, interval) -> {
      if (value.isPresent() && value.get()) {
        final var duration = interval.duration();
        if (duration.shorterThan(minDur) || duration.longerThan(maxDur)) {
          return Optional.of(false);
        } else {
          return Optional.of(true);
        }
      }
      return value;
    }));
  }

  /**
   * Shifts the true segments' start and end points by the given durations.
   *
   * Also shifts the false segments' start and end points by the opposite durations;
   * i.e. the start is shifted by `fromEnd` and the end is shifted by `fromStart`.
   * This keeps the falses in line with the trues when there are no gaps,
   * and expands/shrinks gaps accordingly when there are.
   *
   * @param fromStart duration to shift false -> true rising edges
   * @param fromEnd duration to shift true -> false falling edges
   * @return a new Windows
   */
  public Windows shiftBy(Duration fromStart, Duration fromEnd) {
    final var map = new IntervalMap<Boolean>();

    for (final var segment : this.segments) {
      final var interval = segment.interval();

      final var shiftedInterval = (segment.value()) ? (
          Interval.between(
              interval.start.saturatingPlus(fromStart), interval.startInclusivity,
              interval.end.saturatingPlus(fromEnd),     interval.endInclusivity)
      ) : (
          Interval.between(
              interval.start.saturatingPlus(fromEnd), interval.startInclusivity,
              interval.end.saturatingPlus(fromStart), interval.endInclusivity)
      );

      // SAFETY: If two intervals overlap, they have the same value.
      // Otherwise we'd need to make sure to `or` the overlap together.
      map.insertInPlace(Segment.of(shiftedInterval, segment.value()), 0);
    }

    return new Windows(map);
  }

  /**
   * Converts this into a Spans object, where each true segment is a Span.
   *
   * Gaps are treated the same as false.
   */
  public Spans intoSpans() {
    final var trueIntervals = StreamSupport.stream(this.segments.spliterator(), false).flatMap($ -> {
      if ($.value()) {
        return Stream.of($.interval());
      } else {
        return Stream.of();
      }
    }).toList();
    return new Spans(trueIntervals);
  }

  ////// DELEGATED METHODS

  /** Delegated to {@link IntervalMap#set(Interval, Object)} */
  public Windows set(final Interval interval, final boolean value) {
    return new Windows(segments.set(interval, value));
  }

  /** Delegated to {@link IntervalMap#set(List, Object)} */
  public Windows set(final List<Interval> intervals, final boolean value) {
    return new Windows(segments.set(intervals, value));
  }

  /** Delegated to {@link IntervalMap#set(IntervalMap)} */
  public Windows set(final Windows other) {
    return new Windows(segments.set(other.segments));
  }

  /** Delegated to {@link IntervalMap#unset(Interval...)} */
  public Windows unset(final Interval... intervals) {
    return new Windows(segments.unset(intervals));
  }

  /** Delegated to {@link IntervalMap#unset(List)} */
  public Windows unset(final List<Interval> intervals) {
    return new Windows(segments.unset(intervals));
  }

  /** Delegated to {@link IntervalMap#select(Interval...)} */
  public Windows select(final Interval... intervals) {
    return new Windows(segments.select(intervals));
  }

  /** Delegated to {@link IntervalMap#select(List)} */
  public Windows select(final List<Interval> intervals) {
    return new Windows(segments.select(intervals));
  }

  /** Delegated to {@link IntervalMap#get(int)} */
  public Segment<Boolean> get(final int index) {
    return segments.get(index);
  }

  /** Delegated to {@link IntervalMap#size()} */
  public int size() {
    return segments.size();
  }

  /** Delegated to {@link IntervalMap#isEmpty()} */
  public boolean isEmpty() {
    return segments.isEmpty();
  }

  /** Delegated to {@link IntervalMap#iterator()} */
  @Override
  public Iterator<Segment<Boolean>> iterator() {
    return segments.iterator();
  }

  /** Delegated to {@link IntervalMap#iterateEqualTo(Object)} */
  public Iterable<Interval> iterateEqualTo(final boolean value) {
    return segments.iterateEqualTo(value);
  }

  /** Delegated to {@link IntervalMap#stream} */
  public Stream<Segment<Boolean>> stream() {
    return segments.stream();
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof final Windows w)) return false;
    return segments.equals(w.segments);
  }

  @Override
  public String toString() {
    return segments.toString();
  }
}
