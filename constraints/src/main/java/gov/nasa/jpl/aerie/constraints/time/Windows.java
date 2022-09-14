package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public final class Windows implements Iterable<Segment<Boolean>> {
  private final IntervalMap<Boolean> segments;

  public Windows() {
    this.segments = IntervalMap.of();
  }

  @SafeVarargs
  public Windows(final Segment<Boolean>... segments) {
    this.segments = IntervalMap.of(segments);
  }

  public Windows(final List<Segment<Boolean>> segments) {
    this.segments = IntervalMap.of(segments);
  }

  public Windows(final Interval interval, final boolean value) {
    this.segments = IntervalMap.of(interval, value);
  }

  public Windows(final IntervalMap<Boolean> segments) {
    this.segments = segments;
  }

  public Windows(final boolean bool) {
    this.segments = IntervalMap.of(bool);
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
    return new Windows(this.segments.map(b -> !b));
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
    var index = 0;
    for (final var segment : this.segments) {
      if (!segment.value()) continue;

      // Every true segment can be addressed in two ways: a nonnegative offset (from the left)
      // or a negative offset (from the right). Check both.
      if ((index != indexToRemove) && (index != this.segments.size() - indexToRemove)) {
        index += 1;
        continue;
      }

      return new Windows(this.segments.set(segment.interval(), false));
    }

    return this;
  }

  public Windows keepTrueSegment(final int indexToKeep) {
    final var builder = IntervalMap.<Boolean>builder();

    var index = 0;
    for (final var segment : this.segments) {
      if (!segment.value()) {
        builder.add(segment.interval(), false);
        continue;
      }

      // Every true segment can be addressed in two ways: a nonnegative offset (from the left)
      // or a negative offset (from the right). Check both.
      if ((index != indexToKeep) && (index != this.segments.size() - indexToKeep)) {
        builder.add(segment.interval(), false);
        index += 1;
        continue;
      }

      builder.add(segment.interval(), true);
      index += 1;
    }

    return new Windows(builder.build());
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

  public boolean includesPoint(final long quantity, final Duration unit) {
    return this.includes(new Windows(Interval.at(quantity, unit), true));
  }

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
      if (!value) return false;

      final var duration = interval.duration();
      return !(duration.shorterThan(minDur) || duration.longerThan(maxDur));
    }));
  }

  public Windows shiftBy(Duration fromStart, Duration fromEnd){
    final var builder = IntervalMap.<Boolean>builder();

    for (final var segment : this.segments) {
      final var interval = segment.interval();

      final var shiftedInterval = (segment.value()) ? (
          Interval.between(
              interval.start.plus(fromStart), interval.startInclusivity,
              interval.end.plus(fromEnd),     interval.endInclusivity)
      ) : (
          Interval.between(
              interval.start.plus(fromEnd), interval.startInclusivity,
              interval.end.plus(fromStart), interval.endInclusivity)
      );

      // SAFETY: If two intervals overlap, they have the same value.
      // Otherwise we'd need to make sure to `or` the overlap together.
      builder.add(shiftedInterval, segment.value());
    }

    return new Windows(builder.build());
  }

  public Spans intoSpans() {
    return new Spans(StreamSupport
        .stream(this.segments.spliterator(), false)
        .filter($ -> $.value())
        .map($ -> $.interval())
        .toList());
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

  public Optional<Pair<Duration, Interval.Inclusivity>> minValidTimePoint() {
    return segments.minValidTimePoint();
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> maxValidTimePoint() {
    return segments.maxValidTimePoint();
  }

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

  public boolean isAllEqualTo(final Boolean value) {
    return segments.isAllEqualTo(value);
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
