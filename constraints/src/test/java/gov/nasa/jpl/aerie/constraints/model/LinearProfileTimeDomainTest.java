package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import org.junit.jupiter.api.Test;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;

public class LinearProfileTimeDomainTest {

  @Test
  public void testChangePoints() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  5, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Window.between( 5, Inclusive,  8, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 10, Inclusive, SECONDS), 3, 0),
        new LinearProfilePiece(Window.between(10, Exclusive, 15, Exclusive, SECONDS), 3, 1),
        new LinearProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = profile.changePoints(Window.between(9, 16, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between(10, Exclusive, 15, Inclusive, SECONDS));

    assertEquivalent(result, expected);
  }

  @Test
  public void testLessThan() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Window.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Window.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.lessThan(other, Window.between(9, 14, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between(9, Inclusive, 14, Exclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testLessThanOrEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Window.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Window.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.lessThanOrEqualTo(other, Window.between(0, 8, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between( 0, Inclusive,  2, Inclusive, SECONDS));
    expected.add(Window.between( 6, Inclusive, 8, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testGreaterThan() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Window.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Window.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.greaterThan(other, Window.between(2, 15, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between( 2, Exclusive,  6, Exclusive, SECONDS));
    expected.add(Window.between(14, Exclusive, 15, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testGreaterThanOrEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Window.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Window.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.greaterThanOrEqualTo(other, Window.between(8, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.at(8, SECONDS));
    expected.add(Window.between(14, Inclusive, 20, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Window.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Window.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.equalTo(other, Window.between(7, 17, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between( 7, Inclusive,  8, Inclusive, SECONDS));
    expected.add(Window.at(14, SECONDS));
    expected.add(Window.between( 16, Inclusive,  17, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }

  @Test
  public void testNotEqualTo() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  4, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 4, Inclusive,  8, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 12, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  2, Exclusive, SECONDS),  0,  1),
        new LinearProfilePiece(Window.between( 2, Inclusive,  4, Exclusive, SECONDS),  2,  0),
        new LinearProfilePiece(Window.between( 4, Inclusive,  6, Exclusive, SECONDS),  2,  1),
        new LinearProfilePiece(Window.between( 6, Inclusive, 12, Exclusive, SECONDS),  4,  0),
        new LinearProfilePiece(Window.between(12, Inclusive, 16, Exclusive, SECONDS),  4, -1),
        new LinearProfilePiece(Window.between(16, Inclusive, 20, Inclusive, SECONDS),  0,  0)
    );

    final var result = profile.notEqualTo(other, Window.between(2, 11, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between(2, Exclusive, 6, Exclusive, SECONDS));
    expected.add(Window.between(8, Exclusive, 11, Inclusive, SECONDS));

    assertEquivalent(expected, result);
  }
}
