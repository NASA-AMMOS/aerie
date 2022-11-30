package gov.nasa.jpl.aerie.constraints.profile;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.IntervalContainer;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public interface Windows extends Profile<Boolean>, IntervalContainer<Windows> {
  static Windows from(final boolean constant) {
    return Profile.from(constant)::stream;
  }

  default Windows not() {
    return mapValues($ -> !$)::stream;
  }

  default Windows and(final Profile<Boolean> other) {
    return Profile.map2Values(
        this, other,
        BinaryOperation.fromCases(
            l -> l ? Optional.empty() : Optional.of(false),
            r -> r ? Optional.empty() : Optional.of(false),
            (l, r) -> Optional.of(l && r)
        )
    )::stream;
  }

  default Windows or(final Profile<Boolean> other) {
    return Profile.map2Values(
        this, other,
        BinaryOperation.fromCases(
            l -> l ? Optional.of(true) : Optional.empty(),
            r -> r ? Optional.of(true) : Optional.empty(),
            (l, r) -> Optional.of(l || r)
        )
    )::stream;
  }

  default Windows add(final Profile<Boolean> other) {
    return Profile.map2Values(
        this, other,
        BinaryOperation.combineDefaultIdentity((l, r) -> l || r)
    )::stream;
  }

  default Windows trueSubsetContainedIn(final Interval interval) {
    return bounds -> this.stream(bounds).map(
      $ -> {
        if (interval.contains($.interval())) {
          return $;
        } else {
          return $.mapValue(v -> false);
        }
      }
    );
  }

  default Windows removeTrueSegment(final int index) {
    return bounds -> {
      final Stream<Segment<Boolean>> reversedStream;
      final int reversedIndex;
      if (index >= 0) {
        reversedStream = this.stream(bounds);
        reversedIndex = index;
      } else {
        final var list = this.stream(bounds).toList();
        Collections.reverse(list);
        reversedStream = list.stream();
        reversedIndex = -index - 1;
      }
      final AtomicInteger segmentCounter = new AtomicInteger(0);
      final var intermediateStream = reversedStream.map(
          $ -> {
            if ($.value() && segmentCounter.getAndIncrement() == index) {
              return $.mapValue(v -> false);
            } else {
              return $;
            }
          }
      );
      if (index >= 0) {
        return intermediateStream;
      } else {
        final var list = intermediateStream.toList();
        Collections.reverse(list);
        return list.stream();
      }
    };
  }

  default Windows keepTrueSegment(final int index) {
    return bounds -> {
      final Stream<Segment<Boolean>> reversedStream;
      final int reversedIndex;
      if (index >= 0) {
        reversedStream = this.stream(bounds);
        reversedIndex = index;
      } else {
        final var list = this.stream(bounds).toList();
        Collections.reverse(list);
        reversedStream = list.stream();
        reversedIndex = -index - 1;
      }
      final AtomicInteger segmentCounter = new AtomicInteger(0);
      final var intermediateStream = reversedStream.map(
          $ -> {
            if ($.value() && segmentCounter.getAndIncrement() != index) {
              return $.mapValue(v -> false);
            } else {
              return $;
            }
          }
      );
      if (index >= 0) {
        return intermediateStream;
      } else {
        final var list = intermediateStream.toList();
        Collections.reverse(list);
        return list.stream();
      }
    };
  }

  default boolean includes(final Profile<Boolean> other, final Interval bounds) {
    final var extraSegments = Profile.map2Values(
        this, other,
        BinaryOperation.fromCases(
            $ -> Optional.of(false),
            Optional::of,
            (l, r) -> Optional.of(r && !l)
        )
    );

    // anywhere where the above has true means other was not included in this.
    return extraSegments.stream(bounds).noneMatch(Segment::value);
  }

  default Windows filterByDuration(final Duration min, final Duration max) {
    return bounds -> this.stream(bounds).map(
      $ -> {
        if ($.value()) {
          final var duration = $.interval().duration();
          if (duration.shorterThan(min) || duration.longerThan(max)) {
            return Segment.of($.interval(), false);
          } else {
            return $;
          }
        } else {
          return $;
        }
      }
    );
  }

  default Windows shiftEdges(final Duration shiftRising, final Duration shiftFalling) {
    return bounds -> {
      final var newBounds = Interval.between(
          Duration.min(bounds.start.minus(shiftRising), bounds.start.minus(shiftFalling)),
          bounds.startInclusivity,
          Duration.max(bounds.end.minus(shiftRising), bounds.end.minus(shiftFalling)),
          bounds.endInclusivity
      );
      return this.stream(newBounds).map(
          $ -> {
            if ($.value()) {
              return $.mapInterval(
                  interval -> Interval.between(
                      interval.start.saturatingPlus(shiftRising),
                      interval.startInclusivity,
                      interval.end.saturatingPlus(shiftFalling),
                      interval.endInclusivity
                  )
              );
            } else {
              return $.mapInterval(
                  interval -> Interval.between(
                      interval.start.saturatingPlus(shiftFalling),
                      interval.startInclusivity,
                      interval.end.saturatingPlus(shiftRising),
                      interval.endInclusivity
                  )
              );
            }
          }
      ).filter($ -> !$.interval().isEmpty());
    };
  }

  @Override
  default Windows starts() {
    return specificEdges(false, true);
  }

  @Override
  default Windows ends() {
    return specificEdges(true, false);
  }

  default Spans intoSpans(final Interval bounds) {
    return new Spans(
        this.stream(bounds)
            .filter(Segment::value)
            .map(Segment::interval)
    );
  }
}
