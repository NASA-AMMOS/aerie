package gov.nasa.jpl.aerie.constraints.profile;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;

/**
 * A linear equation in point-slope form.
 */
public final class LinearEquation {
  public final Duration initialTime;
  public final double initialValue;
  public final double rate;

  public LinearEquation(final Duration initialTime, final double initialValue, final double rate) {
    this.initialTime = initialTime;
    this.initialValue = initialValue;
    this.rate = rate;
  }

  public LinearEquation(final double value) {
    this(Duration.ZERO, value, 0);
  }

  public double valueAt(final Duration time) {
    return this.initialValue + this.rate*(time.minus(this.initialTime)).ratioOver(Duration.SECOND);
  }

  public LinearEquation shiftInitialTime(final Duration newInitialTime) {
    return new LinearEquation(
        newInitialTime,
        this.initialValue + newInitialTime.minus(this.initialTime).ratioOver(Duration.SECOND)*this.rate,
        this.rate
    );
  }

  public boolean changing() {
    return this.rate != 0;
  }

  private Optional<Duration> intersectionPointWith(final LinearEquation other) {
    if (this.rate == other.rate) return Optional.empty();
    return Optional.of(
        this.initialTime.plus(
            Duration.roundNearest(
                (other.valueAt(this.initialTime) - this.initialValue) / (this.rate - other.rate),
                Duration.SECONDS
            )
        )
    );
  }

  public static Windows compare(
      final LinearEquation left,
      final LinearEquation right,
      final BiPredicate<Double, Double> predicate
  ) {
    final var intersectionOption = left.intersectionPointWith(right);

    if (intersectionOption.isEmpty()) {
      final var resultEverywhere = predicate.test(left.initialValue, right.valueAt(left.initialTime));
      return Windows.from(resultEverywhere);
    } else {
      final var intersection = intersectionOption.get();
      final var oneSecondBefore = intersection.minus(Duration.SECOND);
      final var oneSecondAfter = intersection.plus(Duration.SECOND);
      final var result = IntervalMap.of(
          Segment.of(
              Interval.between(Duration.MIN_VALUE, Inclusive, intersection, Exclusive),
              predicate.test(left.valueAt(oneSecondBefore), right.valueAt(oneSecondBefore))
          ),
          Segment.of(
              Interval.at(intersection),
              predicate.test(left.valueAt(intersection), right.valueAt(intersection))
          ),
          Segment.of(
              Interval.between(intersection, Exclusive, Duration.MAX_VALUE, Inclusive),
              predicate.test(left.valueAt(oneSecondAfter), right.valueAt(oneSecondAfter))
          )
      );
      return result::stream;
    }
  }

  public String toString() {
    return String.format(
            "{\n" +
            "  Initial Time: %s\n" +
            "  Initial Value: %s\n" +
            "  Rate: %s\n" +
            "}",
            this.initialTime,
            this.initialValue,
            this.rate
    );
  }

  @Override
  public boolean equals(final Object obj) {
    if(!(obj instanceof final LinearEquation other)) return false;

    return this.valueAt(Duration.ZERO) == other.valueAt(Duration.ZERO)
        && this.valueAt(Duration.MINUTE) == other.valueAt(Duration.MINUTE);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.initialValue, this.initialTime, this.rate);
  }
}
