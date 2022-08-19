package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Windows extends IntervalMap<Boolean> {

  public Windows() {
    super();
  }

  public Windows(IntervalAlgebra a) { //only for testing.
    super(a);
  }

  public Windows(final Windows other) {
    super(other);
  }

  @SafeVarargs
  public Windows(final Pair<Interval, Boolean>... windows) {
    super(windows);
  }

  public Windows(final Interval interval, final boolean value) {
    super(interval, value);
  }

  private Windows(final IntervalMap<Boolean> from) {
    super(from.alg, from.segments);
  }

  public static Windows definedEverywhere(final Interval interval, final boolean value) {
    final var result = new Windows(Interval.FOREVER, !value);
    result.set(interval, value);
    return result;
  }

  public static Windows definedEverywhere(final List<Interval> intervals, final boolean value) {
    final var result = new Windows(Interval.FOREVER, !value);
    result.setAll(intervals, value);
    return result;
  }

  public void setAllTrue(final List<Interval> intervals) {
    for (final var interval: intervals) {
      this.set(interval, true);
    }
  }

  public void setTrue(final Interval... intervals) {
    this.setAllTrue(Arrays.stream(intervals).toList());
  }

  public Windows and(final Windows other) {
    return new Windows(IntervalMap.map2(
        this, other,
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

  public Windows and(final Interval other) {
    return this.and(definedEverywhere(other, true));
  }

  public Windows or(final Windows other) {
    return new Windows(IntervalMap.map2(
        this, other,
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

  public Windows or(final Interval other) {
    return this.or(definedEverywhere(other, true));
  }

  public Windows add(final Windows other) {
    return new Windows(IntervalMap.map2(
        this, other,
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
    return new Windows(this.map($ -> $.map(b -> !b)));
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> minValidTimePoint(){
    if(!this.isEmpty()) {
      final var window = this.segments.get(0).getKey();
      return Optional.of(Pair.of(window.start, window.startInclusivity));
    } else{
      return Optional.empty();
    }
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> maxValidTimePoint(){
    if(!isEmpty()) {
      final var window = this.segments.get(this.segments.size() - 1).getKey();
      return Optional.of(Pair.of(window.end, window.endInclusivity));
    } else{
      return Optional.empty();
    }
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> minTrueTimePoint(){
    for (final var segment: this) {
      if (segment.getValue()) {
        final var window = segment.getKey();
        return Optional.of(Pair.of(window.start, window.startInclusivity));
      }
    }
    return Optional.empty();
  }

  public Optional<Pair<Duration, Interval.Inclusivity>> maxTrueTimePoint(){
    for (int i = this.segments.size() - 1; i >= 0; i--) {
      final var segment = this.segments.get(i);
      if (segment.getValue()) {
        final var window = segment.getKey();
        return Optional.of(Pair.of(window.end, window.endInclusivity));
      }
    }
    return Optional.empty();
  }

  public Windows trueSubsetContainedIn(final Interval interval) {
    final var result = new Windows(interval, false);
    for (final var segment: this) {
      if (segment.getValue() && interval.contains(segment.getKey())) {
        result.setTrue(segment.getKey());
      }
    }
    return result;
  }

  public Windows removeTrueSegment(final int indexToRemove) {
    final var result = new Windows(this);
    if (indexToRemove >= 0) {
      int index = 0;
      for (final var segment : result) {
        if (segment.getValue()) {
          if (index == indexToRemove) {
            this.setInternal(segment.getKey(), false, Math.max(index - 1,  0));
            break;
          } else {
            index += 1;
          }
        }
      }
    } else {
      int index = -1;
      for (int i = this.segments.size() - 1; i >= 0; i--) {
        final var segment = this.segments.get(i);
        if (segment.getValue()) {
          if (index == indexToRemove) {
            this.setInternal(segment.getKey(), false, Math.max(i - 1, 0));
            break;
          } else {
            index -= 1;
          }
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
        this,
        other,
        ($original, $other) -> $other.map($ -> !$ || ($original.isPresent() && $original.get()))
    );

    //anywhere where the above has false means inclusion wasn't perfect, so squash and get a truth value:
    return StreamSupport.stream(inclusion.spliterator(), false).allMatch(Pair::getValue);
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


    /*final var ret = new NewWindows();
    StreamSupport
        .stream(windows.ascendingOrder().spliterator(), false)
        .filter(win -> win.getKey().duration().noShorterThan(minDur) && win.getKey().duration().noLongerThan(maxDur))
        .forEach(interval -> ret.add(interval.getKey(), interval.getValue()));
    return ret;*/

    return new Windows(this.contextMap((value, interval) -> {
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
    final Windows result = new Windows(this.map($ -> $.map(b -> false)));
    for (final var segment: segments) {
      if (segment.getValue()) {
        final var interval = segment.getKey();
        if (interval.end.plus(fromEnd).noShorterThan(interval.start.plus(fromStart))) {
          result.set(Interval.between(
              interval.start.plus(fromStart),
              interval.startInclusivity,
              interval.end.plus(fromEnd),
              interval.endInclusivity), true);
        }
      }
    }
    return result;
  }

  public TrueWindowsIterable iterateTrue() {
    return new TrueWindowsIterable(this);
  }

  public Spliterator<Interval> spliterateTrue() {
    return StreamSupport.stream(this.spliterator(), false).flatMap(pair -> {
      if (pair.getValue()) {
        return Stream.of(pair.getKey());
      } else {
        return Stream.of();
      }
    }).spliterator();
  }

  public boolean isAllFalse() {
    for (final var segment: segments) {
      if (segment.getValue()) {
        return false;
      }
    }
    return true;
  }

  public Spans intoSpans() {
    final var trueIntervals = StreamSupport.stream(this.spliterator(), false).flatMap($ -> {
      if ($.getValue()) {
        return Stream.of($.getKey());
      } else {
        return Stream.of();
      }
    }).toList();
    return new Spans(trueIntervals);
  }
}
