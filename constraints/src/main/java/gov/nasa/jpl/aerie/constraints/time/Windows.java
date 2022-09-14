package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Windows implements Iterable<Segment<Boolean>> {

  private final IntervalMap<Boolean> segments;

  public Windows() {
    this.segments = new IntervalMap<>();
  }

  public Windows(final Windows other) {
    this.segments = new IntervalMap<>(other.segments);
  }

  @SafeVarargs
  public Windows(final Segment<Boolean>... segments) {
    this.segments = new IntervalMap<>(segments);
  }

  public Windows(final List<Segment<Boolean>> segments) {
    this.segments = new IntervalMap<>(segments);
  }

  public Windows(final Interval interval, final boolean value) {
    this.segments = new IntervalMap<>(interval, value);
  }

  public Windows(final IntervalMap<Boolean> segments) {
    this.segments = segments;
  }

  public Windows(final Boolean bool) {
    this.segments = new IntervalMap<>(bool);
  }

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

  public Windows not() {
    //should not be a subtraction because then if it was null originally, then subtracting original from forever
    //  yields true where once was null, which isn't good. we want a simple inversion of true and false here, without
    //  filling nulls.
    return new Windows(this.segments.map($ -> $.map(b -> !b)));
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> minTrueTimePoint(){
    for (final var segment: this.segments) {
      if (segment.value()) {
        final var window = segment.interval();
        return Optional.of(Pair.of(window.start, window.startInclusivity));
      }
    }
    return Optional.empty();
  }

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

  public Windows trueSubsetContainedIn(final Interval interval) {
    var result = new Windows(interval, false);
    for (final var segment: this.segments) {
      if (segment.value() && interval.contains(segment.interval())) {
        result = result.set(segment.interval(), true);
      }
    }
    return result;
  }

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
    return new Windows(this);
  }

  public Windows keepTrueSegment(final int indexToKeep) {
    var result = new Windows(this);
    if (indexToKeep >= 0) {
      int index = 0;
      for (final var interval : this.segments.iterateEqualTo(true)) {
        if (index != indexToKeep) {
          result.segments.setInternal(interval, false, 0);
        }
        index += 1;
      }
    } else {
      int index = -1;
      for (int i = this.segments.size() - 1; i >= 0; i--) {
        final var segment = this.segments.get(i);
        if (segment.value()) {
          if (index != indexToKeep) {
            result.segments.setInternal(segment.interval(), false, 0);
          }
          index -= 1;
        }
      }
    }
    return result;
  }

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

  public boolean includes(final Interval probe) {
    return this.includes(new Windows(probe, true));
  }

  /**
   * Sets true segments shorter than `minDur` or longer than `maxDur` to false.
   *

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


    return new Windows(this.segments.contextMap((value, interval) -> {
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

  public Windows shiftBy(Duration fromStart, Duration fromEnd){
    final Windows result = new Windows(this.segments.map($ -> $.map(b -> false)));
    for (final var segment: this.segments) {
      if (segment.value()) {
        final var interval = segment.interval();
        if (interval.end.plus(fromEnd).noShorterThan(interval.start.plus(fromStart))) {
          result.segments.setInternal(Interval.between(
              interval.start.plus(fromStart),
              interval.startInclusivity,
              interval.end.plus(fromEnd),
              interval.endInclusivity), true, 0);
        }
      }
    }
    return result;
  }

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

  public Windows set(final Interval interval, final Boolean value) {
    return new Windows(segments.set(interval, value));
  }

  public Windows set(final List<Interval> intervals, final Boolean value) {
    return new Windows(segments.set(intervals, value));
  }

  public Windows set(final Windows other) {
    return new Windows(segments.set(other.segments));
  }

  public Windows unset(final Interval interval) {
    return new Windows(segments.unset(interval));
  }

  public Windows unset(final List<Interval> intervals) {
    return new Windows(segments.unset(intervals));
  }

  public Windows unset(final IntervalMap<?> other) {
    return new Windows(segments.unset(other));
  }

  public Windows select(final Interval bounds) {
    return new Windows(segments.select(bounds));
  }

  public Windows select(final List<Interval> intervals) {
    return new Windows(segments.select(intervals));
  }

  /** Delegated to {@link IntervalMap#get(int)} */
  public Segment<Boolean> get(final int index) {
    return segments.get(index);
  }

  public int size() {
    return segments.size();
  }

  public boolean isEmpty() {
    return segments.isEmpty();
  }

  @Override
  public Iterator<Segment<Boolean>> iterator() {
    return segments.iterator();
  }

  public Iterable<Interval> iterateEqualTo(final Boolean value) {
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
