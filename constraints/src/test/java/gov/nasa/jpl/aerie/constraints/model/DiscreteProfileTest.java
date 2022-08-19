package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class DiscreteProfileTest {

  @Test
  public void testEqualTo() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between( 5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var other = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between( 5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.equalTo(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(Interval.between(0, 20, SECONDS), false);
    expected.setTrue(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS));
    expected.setTrue(Interval.between(15, Inclusive, 20, Inclusive, SECONDS));

    assertIterableEquals(expected, result);
  }

  @Test
  public void testNotEqualTo() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between( 5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var other = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between( 5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.notEqualTo(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(Interval.between(0, 20, SECONDS), false);
    expected.setTrue(Interval.between( 5, Inclusive, 15, Exclusive, SECONDS));

    assertIterableEquals(expected, result);
  }

  @Test
  public void testChangePoints() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between( 5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.changePoints(Interval.between(0, 20, SECONDS));

    final var expected = new Windows(Interval.between(0, 20, SECONDS), false);
    expected.setTrue(Interval.at( 5, SECONDS));
    expected.setTrue(Interval.at(10, SECONDS));
    expected.setTrue(Interval.at(15, SECONDS));

    assertIterableEquals(expected, result);
  }

  @Test
  public void testTransitions() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between( 5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Interval.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.transitions(SerializedValue.of(true), SerializedValue.of(false), Interval.between(0, 20, SECONDS));

    final var expected = new Windows(Interval.between(0, 20, SECONDS), false);
    expected.setTrue(Interval.at( 5, SECONDS));
    expected.setTrue(Interval.at(15, SECONDS));

    assertIterableEquals(expected, result);
  }
}
