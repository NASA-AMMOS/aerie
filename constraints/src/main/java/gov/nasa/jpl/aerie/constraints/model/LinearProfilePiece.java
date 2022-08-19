package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.at;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;

public final class LinearProfilePiece {
  public final Interval interval;
  public final double initialValue;
  public final double rate;

  public LinearProfilePiece(final Interval interval, final double initialValue, final double rate) {
    this.interval = interval;
    this.initialValue = initialValue;
    this.rate = rate;
  }

  public double finalValue() {
    return this.valueAt(this.interval.end);
  }

  public double valueAt(final Duration time) {
    return this.initialValue + this.rate*(time.minus(this.interval.start)).ratioOver(Duration.SECOND);
  }

  public Windows changePoints() {
    return new Windows(this.interval, this.rate != 0);
  }

  private Optional<Duration> intersectionPointWith(final LinearProfilePiece other) {
    if (this.rate == other.rate) return Optional.empty();
    return Optional.of(
        this.interval.start.plus(
            Duration.roundNearest(
                (other.valueAt(this.interval.start) - this.initialValue) / (this.rate - other.rate)*1000000,
                Duration.MICROSECONDS)
        )
    );
  }

  public List<Pair<Interval, Boolean>> intervalsLessThan(final LinearProfilePiece other) {
    return getInequalityIntervals(other, (l, r) -> l < r, Exclusive);
  }

  public List<Pair<Interval, Boolean>> intervalsLessThanOrEqualTo(final LinearProfilePiece other) {
    return getInequalityIntervals(other, (l, r) -> l <= r, Inclusive);
  }

  public List<Pair<Interval, Boolean>> intervalsGreaterThan(final LinearProfilePiece other) {
    return getInequalityIntervals(other, (l, r) -> l > r, Exclusive);
  }

  public List<Pair<Interval, Boolean>> intervalsGreaterThanOrEqualTo(final LinearProfilePiece other) {
    return getInequalityIntervals(other, (l, r) -> l >= r, Inclusive);
  }

  private List<Pair<Interval, Boolean>> getInequalityIntervals(
      final LinearProfilePiece other,
      final BiFunction<Double, Double, Boolean> op,
      final Interval.Inclusivity intersectionInclusivity)
  {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);


    if (intersection.isEmpty()) {
        return List.of(Pair.of(
            overlap,
            op.apply(this.initialValue, other.valueAt(this.interval.start))
        ));
    } else {
      final var t = intersection.get();
      final var inequalityBeforeIntersect = op.apply(this.valueAt(t.minus(Duration.SECOND)), other.valueAt(t.minus(Duration.SECOND)));

      if (overlap.contains(t)) {
        final Interval.Inclusivity flippedInclusivity;
        if (inequalityBeforeIntersect) {
          flippedInclusivity = intersectionInclusivity;
        } else {
          flippedInclusivity = intersectionInclusivity.opposite();
        }
        return List.of(
            Pair.of(interval(overlap.start, overlap.startInclusivity, t, flippedInclusivity), inequalityBeforeIntersect),
            Pair.of(interval(t, flippedInclusivity.opposite(), overlap.end, overlap.endInclusivity), !inequalityBeforeIntersect)
        );
      } else {
        return List.of(Pair.of(overlap, inequalityBeforeIntersect));
      }
    }
  }

  public List<Pair<Interval, Boolean>> intervalsEqualTo(final LinearProfilePiece other) {
    final var overlap = Interval.intersect(this.interval, other.interval);
    final var intersection = this.intersectionPointWith(other);

    if (intersection.isEmpty()) {
      return List.of(Pair.of(
          overlap,
          this.initialValue == other.valueAt(this.interval.start)
      ));
    } else {
      final var t = intersection.get();
      if (overlap.contains(t)) {
        return List.of(
            Pair.of(interval(overlap.start, overlap.startInclusivity, t, Exclusive), false),
            Pair.of(at(t), true),
            Pair.of(interval(t, Exclusive, overlap.end, overlap.endInclusivity), false)
        );
      } else {
        return List.of(Pair.of(overlap, false));
      }
    }
  }

  public List<Pair<Interval, Boolean>> intervalsNotEqualTo(final LinearProfilePiece other) {
    return this.intervalsEqualTo(other).stream().map(
        $ -> Pair.of($.getKey(), !$.getValue())
    ).toList();
  }

  public static List<Pair<Interval, Boolean>> lessThan(LinearProfilePiece left, LinearProfilePiece right) {
    return left.intervalsLessThan(right);
  }

  public static List<Pair<Interval, Boolean>> lessThanOrEqualTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.intervalsLessThanOrEqualTo(right);
  }

  public static List<Pair<Interval, Boolean>> greaterThan(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.intervalsGreaterThan(right);
  }

  public static List<Pair<Interval, Boolean>> greaterThanOrEqualTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.intervalsGreaterThanOrEqualTo(right);
  }

  public static List<Pair<Interval, Boolean>> equalTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.intervalsEqualTo(right);
  }

  public static List<Pair<Interval, Boolean>> notEqualTo(final LinearProfilePiece left, final LinearProfilePiece right) {
    return left.intervalsNotEqualTo(right);
  }

  public String toString() {
    return String.format(
            "{\n" +
            "  Interval: %s\n" +
            "  Initial Value: %s\n" +
            "  Rate: %s\n" +
            "}",
            this.interval,
            this.initialValue,
            this.rate
    );
  }

  @Override
  public boolean equals(final Object obj) {
    if(!(obj instanceof LinearProfilePiece)) return false;
    final var other = (LinearProfilePiece)obj;

    return this.interval.equals(other.interval) &&
           this.initialValue == other.initialValue &&
           this.rate == other.rate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.initialValue, this.interval, this.rate);
  }
}
