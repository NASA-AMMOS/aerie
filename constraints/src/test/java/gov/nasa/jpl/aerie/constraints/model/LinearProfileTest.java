package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import org.junit.Test;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.Utilities.assertEquivalent;
import static gov.nasa.jpl.aerie.time.Duration.SECONDS;

public class LinearProfileTest {

  @Test
  public void testBasicAddition() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between(0, Inclusive,10, Exclusive, SECONDS), 0, 1),
        new LinearProfilePiece(Window.between(10, Inclusive,20, Inclusive, SECONDS), 10, -2)
    );

    final var other = new LinearProfile(
      new LinearProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), 1, 2),
      new LinearProfilePiece(Window.between(5, Inclusive, 10, Inclusive, SECONDS), 11, -1),
      new LinearProfilePiece(Window.between(10, Exclusive, 15, Exclusive, SECONDS), 6, 3),
      new LinearProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), 21, -2)
    );

    final var result = (LinearProfile)(profile.plus(other));

    final var expected = new LinearProfile(
        new LinearProfilePiece(Window.between(0, Inclusive, 5, Exclusive, SECONDS), 1, 3),
        new LinearProfilePiece(Window.between(5, Inclusive, 10, Exclusive, SECONDS), 16, 0),
        new LinearProfilePiece(Window.between(10, Inclusive, 10, Inclusive, SECONDS), 16, -3),
        new LinearProfilePiece(Window.between(10, Exclusive, 15, Exclusive, SECONDS), 16, 1),
        new LinearProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), 21, -4)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testAdditionWithDifferingBounds() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 5, Exclusive,10, Inclusive, SECONDS), 2, 2),
        new LinearProfilePiece(Window.between(10, Exclusive,20, Inclusive, SECONDS), 12, -1)
    );

    final var other = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  8, Inclusive, SECONDS), 2, 3),
        new LinearProfilePiece(Window.between( 8, Exclusive, 15, Inclusive, SECONDS), 26, -3)
    );

    final var result = (LinearProfile)(profile.plus(other));

    final var expected = new LinearProfile(
        new LinearProfilePiece(Window.between( 5, Exclusive,  8, Inclusive, SECONDS), 19, 5),
        new LinearProfilePiece(Window.between( 8, Exclusive, 10, Inclusive, SECONDS), 34, -1),
        new LinearProfilePiece(Window.between(10, Exclusive, 15, Inclusive, SECONDS), 32, -4)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testMultiplication() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive, 10, Exclusive, SECONDS), 2, 3),
        new LinearProfilePiece(Window.between(10, Inclusive, 20, Exclusive, SECONDS), 3, -5),
        new LinearProfilePiece(Window.between(20, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = (LinearProfile)(profile.times(2));

    final var expected = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive, 10, Exclusive, SECONDS), 4, 6),
        new LinearProfilePiece(Window.between(10, Inclusive, 20, Exclusive, SECONDS), 6, -10),
        new LinearProfilePiece(Window.between(20, Inclusive, 20, Inclusive, SECONDS), -4, 0)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testRate() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive, 10, Exclusive, SECONDS), 2, 3),
        new LinearProfilePiece(Window.between(10, Inclusive, 20, Exclusive, SECONDS), 3, -5),
        new LinearProfilePiece(Window.between(20, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = (LinearProfile)(profile.rate());

    final var expected = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive, 10, Exclusive, SECONDS), 3, 0),
        new LinearProfilePiece(Window.between(10, Inclusive, 20, Exclusive, SECONDS), -5, 0),
        new LinearProfilePiece(Window.between(20, Inclusive, 20, Inclusive, SECONDS), 0, 0)
    );

    assertEquivalent(expected, result);
  }

  @Test
  public void testChangePoints() {
    final var profile = new LinearProfile(
        new LinearProfilePiece(Window.between( 0, Inclusive,  5, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Window.between( 5, Inclusive,  8, Exclusive, SECONDS), 2, 0),
        new LinearProfilePiece(Window.between( 8, Inclusive, 10, Inclusive, SECONDS), 3, 0),
        new LinearProfilePiece(Window.between(10, Exclusive, 15, Exclusive, SECONDS), 3, 1),
        new LinearProfilePiece(Window.between(15, Inclusive, 20, Inclusive, SECONDS), -2, 0)
    );

    final var result = profile.changePoints(Window.between(0, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.at(8, SECONDS));
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

    final var result = profile.lessThan(other, Window.between(0, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between(8, Exclusive, 14, Exclusive, SECONDS));

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

    final var result = profile.lessThanOrEqualTo(other, Window.between(0, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between( 0, Inclusive,  2, Inclusive, SECONDS));
    expected.add(Window.between( 6, Inclusive, 14, Inclusive, SECONDS));
    expected.add(Window.between(16, Inclusive, 20, Inclusive, SECONDS));

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

    final var result = profile.greaterThan(other, Window.between(0, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between( 2, Exclusive,  6, Exclusive, SECONDS));
    expected.add(Window.between(14, Exclusive, 16, Exclusive, SECONDS));

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

    final var result = profile.greaterThanOrEqualTo(other, Window.between(0, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between( 0, Inclusive,  8, Inclusive, SECONDS));
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

    final var result = profile.equalTo(other, Window.between(0, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between( 0, Inclusive,  2, Inclusive, SECONDS));
    expected.add(Window.between( 6, Inclusive,  8, Inclusive, SECONDS));
    expected.add(Window.at(14, SECONDS));
    expected.add(Window.between( 16, Inclusive,  20, Inclusive, SECONDS));

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

    final var result = profile.notEqualTo(other, Window.between(0, 20, SECONDS));

    final var expected = new Windows();
    expected.add(Window.between(2, Exclusive, 6, Exclusive, SECONDS));
    expected.add(Window.between(8, Exclusive, 14, Exclusive, SECONDS));
    expected.add(Window.between(14, Exclusive, 16, Exclusive, SECONDS));

    assertEquivalent(expected, result);
  }
}
