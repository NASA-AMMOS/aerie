package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** An interval represented with absolute datetime bounds, instead of offsets from plan start. */
public record AbsoluteInterval(Optional<Instant> start, Optional<Instant> end, Optional<Interval.Inclusivity> startInclusivity, Optional<Interval.Inclusivity> endInclusivity) {
  public Interval toRelative(final Instant planStart) {
    final Duration relativeStart = start
        .map(instant -> Duration.of(planStart.until(instant, ChronoUnit.MICROS), Duration.MICROSECOND))
        .orElse(Duration.MIN_VALUE);
    final Duration relativeEnd = end
        .map(instant -> Duration.of(planStart.until(instant, ChronoUnit.MICROS), Duration.MICROSECOND))
        .orElse(Duration.MAX_VALUE);

    return Interval.between(
        relativeStart,
        startInclusivity.orElse(Interval.Inclusivity.Inclusive),
        relativeEnd,
        endInclusivity.orElse(Interval.Inclusivity.Inclusive)
    );
  }

  public static AbsoluteInterval FOREVER = new AbsoluteInterval(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
}
