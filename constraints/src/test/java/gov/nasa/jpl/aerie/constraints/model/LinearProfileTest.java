package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECOND;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class LinearProfileTest {

  @Test
  public void testBasicAddition() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between(0, Inclusive,10, Exclusive, SECONDS), 0, 1),
        new LinearProfilePiece(Interval.between(10, Inclusive,20, Inclusive, SECONDS), 10, -2)
    );

    final var other = new LinearProfile(
      new LinearProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), 1, 2),
      new LinearProfilePiece(Interval.between(5, Inclusive, 10, Inclusive, SECONDS), 11, -1),
      new LinearProfilePiece(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), 6, 3),
      new LinearProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), 21, -2)
    );

    final var result = (LinearProfile)(profile.plus(other));

    final var expected = new LinearProfile(
        new LinearProfilePiece(Interval.between(0, Inclusive, 5, Exclusive, SECONDS), 1, 3),
        new LinearProfilePiece(Interval.between(5, Inclusive, 10, Exclusive, SECONDS), 16, 0),
        new LinearProfilePiece(Interval.between(10, Inclusive, 10, Inclusive, SECONDS), 16, -3),
        new LinearProfilePiece(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), 16, 1),
        new LinearProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), 21, -4)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testAdditionWithDifferingBounds() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 5, Exclusive,10, Inclusive, SECONDS), 2, 2),
        new LinearProfilePiece(Interval.between(10, Exclusive,20, Inclusive, SECONDS), 12, -1)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  8, Inclusive, SECONDS), 2, 3),
        new LinearProfilePiece(Interval.between( 8, Exclusive, 15, Inclusive, SECONDS), 26, -3)
    );

    final var result = (LinearProfile)(profile.plus(other));

    final var expected = new LinearProfile(
        new LinearProfilePiece(Interval.between( 5, Exclusive,  8, Inclusive, SECONDS), 19, 5),
        new LinearProfilePiece(Interval.between( 8, Exclusive, 10, Inclusive, SECONDS), 34, -1),
        new LinearProfilePiece(Interval.between(10, Exclusive, 15, Inclusive, SECONDS), 32, -4)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testMultiplication() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive, 10, Exclusive, SECONDS), 2, 3),
        new LinearProfilePiece(Interval.between(10, Inclusive, 20, Exclusive, SECONDS), 3, -5),
        new LinearProfilePiece(Interval.between(20, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = (LinearProfile)(profile.times(2));

    final var expected = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive, 10, Exclusive, SECONDS), 4, 6),
        new LinearProfilePiece(Interval.between(10, Inclusive, 20, Exclusive, SECONDS), 6, -10),
        new LinearProfilePiece(Interval.between(20, Inclusive, 20, Inclusive, SECONDS), -4, 0)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testRate() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive, 10, Exclusive, SECONDS), 2, 3),
        new LinearProfilePiece(Interval.between(10, Inclusive, 20, Exclusive, SECONDS), 3, -5),
        new LinearProfilePiece(Interval.between(20, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = (LinearProfile)(profile.rate());

    final var expected = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive, 10, Exclusive, SECONDS), 3, 0),
        new LinearProfilePiece(Interval.between(10, Inclusive, 20, Exclusive, SECONDS), -5, 0),
        new LinearProfilePiece(Interval.between(20, Inclusive, 20, Inclusive, SECONDS), 0, 0)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testChangePoints() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Interval.between( 5, Inclusive,  8, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 10, Inclusive, SECONDS), 3, 0),
        new LinearProfilePiece(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), 3, 1),
        new LinearProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = profile.changePoints(interval(0, 20, SECONDS));

    final var expected = new Windows(interval(0, 20, SECONDS), false)
    		.set(Interval.at(8, SECONDS), true)
    		.set(Interval.between(10, Exclusive, 15, Inclusive, SECONDS), true);

    assertIterableEquals(expected, result);
  }

  @Test
  public void testLessThan() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Interval.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.lessThan(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(interval(0, 20, SECONDS), false)
    		.set(Interval.between(8, Exclusive, 14, Exclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testLessThanOrEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Interval.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.lessThanOrEqualTo(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(interval(0, 20, SECONDS), false)
    		.set(Interval.between( 0, Inclusive,  2, Inclusive, SECONDS), true)
    		.set(Interval.between( 6, Inclusive, 14, Inclusive, SECONDS), true)
    		.set(Interval.between(16, Inclusive, 20, Inclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testGreaterThan() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Interval.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.greaterThan(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(interval(0, 20, SECONDS), false)
    		.set(Interval.between( 2, Exclusive,  6, Exclusive, SECONDS), true)
    		.set(Interval.between(14, Exclusive, 16, Exclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testGreaterThanOrEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Interval.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.greaterThanOrEqualTo(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(interval(0, 20, SECONDS), false)
    		.set(Interval.between( 0, Inclusive,  8, Inclusive, SECONDS), true)
    		.set(Interval.between(14, Inclusive, 20, Inclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Interval.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.equalTo(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(interval(0, 20, SECONDS), false)
    		.set(Interval.between( 0, Inclusive,  2, Inclusive, SECONDS), true)
    		.set(Interval.between( 6, Inclusive,  8, Inclusive, SECONDS), true)
    		.set(Interval.at(14, SECONDS), true)
    		.set(Interval.between( 16, Inclusive,  20, Inclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testNotEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Interval.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Interval.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Interval.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Interval.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Interval.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.notEqualTo(other, Interval.between(0, 20, SECONDS));

    final var expected = new Windows(interval(0, 20, SECONDS), false)
        .set(Interval.between(2, Exclusive, 6, Exclusive, SECONDS), true)
        .set(Interval.between(8, Exclusive, 14, Exclusive, SECONDS), true)
        .set(Interval.between(14, Exclusive, 16, Exclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }

  @Test
  public void testConvertFromExternalFormat() {
    final var externalProfile = List.of(
        Pair.of(Duration.of(1, SECOND), RealDynamics.linear(1, 1)),
        Pair.of(Duration.of(1, SECOND), RealDynamics.linear(5, -1))
    );

    final var profile = LinearProfile.fromExternalProfile(Duration.of(1, SECOND), externalProfile);

    final var expected = new LinearProfile(
        new LinearProfilePiece(Interval.between(1, Inclusive, 2, Exclusive, SECONDS), 1, 1),
        new LinearProfilePiece(Interval.between(2, Inclusive, 3, Exclusive, SECONDS), 5, -1)
    );

    assertIterableEquals(
        expected.profilePieces, profile.profilePieces
    );
  }
}
