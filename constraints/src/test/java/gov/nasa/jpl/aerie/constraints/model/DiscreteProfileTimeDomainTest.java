package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.at;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class DiscreteProfileTimeDomainTest {
  @Test
  public void testEqualToDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var other = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.equalTo(other, Interval.between(6, 14, SECONDS));

    final var expected = new Windows(Interval.between(6, 14, SECONDS), false);
    assertIterableEquals(expected, result);
  }

  @Test
  public void testNotEqualToDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var other = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.notEqualTo(other, Interval.between(6, 14, SECONDS));

    final var expected = new Windows()
    		.set(Interval.between(6, Inclusive, 14, Inclusive, SECONDS), true);

    assertIterableEquals(expected, result);
  }

  @Test
  public void testChangePointsDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.changePoints(Interval.between(6, 14, SECONDS));

    final var expected = new Windows(
        Segment.of(interval(6, Inclusive, 10, Exclusive, SECONDS), false),
        Segment.of(at(10, SECONDS), true),
        Segment.of(interval(10, Exclusive, 14, Inclusive, SECONDS), false)
    );

    assertIterableEquals(expected, result);
  }

  @Test
  public void testTransitionDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.transitions(
        SerializedValue.of(true),
        SerializedValue.of(false),
        Interval.between(6, 16, SECONDS));

    final var expected = new Windows(
        Segment.of(interval(6, Inclusive, 15, Exclusive, SECONDS), false),
        Segment.of(at(15, SECONDS), true),
        Segment.of(interval(15, Exclusive, 16, Inclusive, SECONDS), false)
    );

    assertIterableEquals(expected, result);
  }
}
