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

    final var expected = new Windows();
    assertEquivalent(expected, result);
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

    final var expected = new Windows();
    expected.add(Interval.between(6, Inclusive, 14, Inclusive, SECONDS));

    assertEquivalent(expected, result);
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

    final var expected = new Windows();
    expected.add(Interval.at(10, SECONDS));

    assertEquivalent(expected, result);
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

    final var expected = new Windows();
    expected.add(Interval.at(15, SECONDS));

    assertEquivalent(expected, result);
  }
}
