package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import org.junit.jupiter.api.Test;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

public class LinearProfileTimeDomainTest {

  @Test
  public void testChangePoints() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Interval.between( 0, Inclusive,  5, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Interval.between( 5, Inclusive,  8, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Interval.between( 8, Inclusive, 10, Inclusive, SECONDS), 3, 0),
        new LinearProfilePiece(Interval.between(10, Exclusive, 15, Exclusive, SECONDS), 3, 1),
        new LinearProfilePiece(Interval.between(15, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = profile.changePoints(Interval.between(9, 16, SECONDS));

    final var expected = new Windows(interval(9, 16, SECONDS), false)
    		.set(Interval.between(10, Exclusive, 15, Inclusive, SECONDS), true);

    assertEquivalent(result, expected);
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

    final var result = profile.lessThan(other, Interval.between(9, 14, SECONDS));

    final var expected = new Windows(interval(9, 14, SECONDS), false)
    		.set(Interval.between(9, Inclusive, 14, Exclusive, SECONDS), true);

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

    final var result = profile.lessThanOrEqualTo(other, Interval.between(0, 8, SECONDS));

    final var expected = new Windows(interval(0, 8, SECONDS), false)
    		.set(Interval.between( 0, Inclusive,  2, Inclusive, SECONDS), true)
    		.set(Interval.between( 6, Inclusive, 8, Inclusive, SECONDS), true);

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

    final var result = profile.greaterThan(other, Interval.between(2, 15, SECONDS));

    final var expected = new Windows(interval(2, 15, SECONDS), false)
    		.set(Interval.between( 2, Exclusive,  6, Exclusive, SECONDS), true)
    		.set(Interval.between(14, Exclusive, 15, Inclusive, SECONDS), true);

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

    final var result = profile.greaterThanOrEqualTo(other, Interval.between(8, 20, SECONDS));

    final var expected = new Windows(interval(8, 20, SECONDS), false)
    		.set(Interval.at(8, SECONDS), true)
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

    final var result = profile.equalTo(other, Interval.between(7, 17, SECONDS));

    final var expected = new Windows(interval(7, 17, SECONDS), false)
    		.set(Interval.between( 7, Inclusive,  8, Inclusive, SECONDS), true)
    		.set(Interval.at(14, SECONDS), true)
    		.set(Interval.between( 16, Inclusive,  17, Inclusive, SECONDS), true);

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

    final var result = profile.notEqualTo(other, Interval.between(2, 11, SECONDS));

    final var expected = new Windows(interval(2, 11, SECONDS), false)
    		.set(Interval.between(2, Exclusive, 6, Exclusive, SECONDS), true)
    		.set(Interval.between(8, Exclusive, 11, Inclusive, SECONDS), true);

    assertEquivalent(expected, result);
  }
}
