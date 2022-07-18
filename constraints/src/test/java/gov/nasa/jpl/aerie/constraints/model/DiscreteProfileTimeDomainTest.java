package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
public class DiscreteProfileTimeDomainTest {
  @Test
  public void testEqualToDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var other = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.equalTo(other, Window.between(6, 14, SECONDS));

    final var expected = new Windows();
    assertEquivalent(expected, result);
  }

  @Test
  public void testNotEqualToDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var other = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.notEqualTo(other, Window.between(6, 14, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between(6, Inclusive, 14, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testChangePointsDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.changePoints(Window.between(6, 14, SECONDS));

    final var expected = new Windows();
    expected.add(Window.at(10, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testTransitionDomain() {
    final var profile = new DiscreteProfile(List.of(
        new DiscreteProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(5, Inclusive, 10, Exclusive, SECONDS), SerializedValue.of(false)),
        new DiscreteProfilePiece(Window.between(10, Inclusive, 15, Exclusive, SECONDS), SerializedValue.of(true)),
        new DiscreteProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), SerializedValue.of(false))
    ));

    final var result = profile.transitions(
        SerializedValue.of(true),
        SerializedValue.of(false),
        Window.between(6, 16, SECONDS));

    final var expected = new Windows();
    expected.add(Window.at(15, SECONDS));

    assertEquivalent(expected, result);
  }
}
