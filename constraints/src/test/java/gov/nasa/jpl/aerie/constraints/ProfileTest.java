package gov.nasa.jpl.aerie.constraints;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.profile.*;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class ProfileTest {
  private static final Interval bounds = Interval.between(0, 10, SECONDS);

  @Test
  public void WindowsValue() {
    final var result = Windows.from(true).evaluate(bounds);
    final var expected = IntervalMap.<Boolean>of(
        Segment.of(bounds, true)
    );
    assertIterableEquals(expected, result);
  }

  @Test
  public void readReifiedMap() {
    final var map = IntervalMap.of(
        Segment.of(Interval.between(0, 1, SECONDS), false),
        Segment.of(Interval.between(9, 10, SECONDS), true)
    );
    final var result = ((Windows) map::stream).not().evaluate(bounds);
    final var expected = IntervalMap.of(
        Segment.of(Interval.between(0, 1, SECONDS), true),
        Segment.of(Interval.between(9, 10, SECONDS), false)
    );
    assertIterableEquals(expected, result);
  }

  @Test
  public void unset() {
    final var map = IntervalMap.of(
        Segment.of(Interval.between(0, 1, SECONDS), false),
        Segment.of(Interval.between(7, 10, SECONDS), true)
    );
    final var result = ((Windows) map::stream).unset(Interval.between(7, 9, SECONDS)).evaluate(bounds);
    final var expected = IntervalMap.of(
        Segment.of(Interval.between(0, 1, SECONDS), false),
        Segment.of(Interval.between(9, Exclusive, 10, Inclusive, SECONDS), true)
    );
    assertIterableEquals(expected, result);
  }

  @Test
  public void map2() {
    final var left = IntervalMap.of(
        Segment.of(Interval.between(-2, 2, SECONDS), true),

        Segment.of(Interval.between(7, 9, SECONDS), false),
        Segment.of(Interval.between(11, 13, SECONDS), true)
    );
    final var right = IntervalMap.of(
        Segment.of(Interval.between(-3, -1, SECONDS), false),
        Segment.of(Interval.between(1, 3, SECONDS), true),

        Segment.of(Interval.between(8, 12, SECONDS), false)
    );

    final var op = BinaryOperation.<Boolean, Boolean, Pair<Boolean, Boolean>>fromCases(
        $ -> Optional.of(Pair.of($, null)),
        $ -> Optional.of(Pair.of(null, $)),
        (l, r) -> Optional.of(Pair.of(l, r))
    );

    final var result = Profile.map2Values(left, right, op).evaluate();

    final var expected = IntervalMap.of(
        Segment.of(Interval.between(-3, Inclusive, -2, Exclusive, SECONDS), Pair.of(null, false)),
        Segment.of(Interval.between(-2, -1, SECONDS), Pair.of(true, false)),
        Segment.of(Interval.between(-1, Exclusive, 1, Exclusive, SECONDS), Pair.of(true, null)),
        Segment.of(Interval.between(1, 2, SECONDS), Pair.of(true, true)),
        Segment.of(Interval.between(2, Exclusive, 3, Inclusive, SECONDS), Pair.of(null, true)),

        Segment.of(Interval.between(7, Inclusive, 8, Exclusive, SECONDS), Pair.of(false, null)),
        Segment.of(Interval.between(8, 9, SECONDS), Pair.of(false, false)),
        Segment.of(Interval.between(9, Exclusive, 11, Exclusive, SECONDS), Pair.of(null, false)),
        Segment.of(Interval.between(11, 12, SECONDS), Pair.of(true, false)),
        Segment.of(Interval.between(12, Exclusive, 13, Inclusive, SECONDS), Pair.of(true, null))
    );

    assertIterableEquals(expected, result);
  }

  @Test
  public void testLessThan() {
    final var bounds = interval(0,20, SECONDS);
    final var expected = Windows.from(false)
                                .set(Profile.from(Interval.between(8, Exclusive, 14, Exclusive, SECONDS), true)).evaluate(bounds);
    final var profile = IntervalMap.of(
        Segment.of(Interval.between( 0, Inclusive,  4, Exclusive, SECONDS), new LinearEquation(Duration.of(0, SECONDS), 0, 1)),
        Segment.of(Interval.between( 4, Inclusive,  8, Exclusive, SECONDS), new LinearEquation(Duration.of( 4, SECONDS),  4,  0)),
        Segment.of(Interval.between( 8, Inclusive, 12, Exclusive, SECONDS), new LinearEquation(Duration.of( 8, SECONDS),  4, -1)),
        Segment.of(Interval.between(12, Inclusive, 16, Exclusive, SECONDS), new LinearEquation(Duration.of(12, SECONDS),  0,  1)),
        Segment.of(Interval.between(16, Inclusive, 20, Inclusive, SECONDS), new LinearEquation(Duration.of(16, SECONDS),  0,  0))
    );

    final var other = IntervalMap.of(
        Segment.of(Interval.between( 0, Inclusive,  2, Exclusive, SECONDS), new LinearEquation(Duration.of( 0, SECONDS),  0,  1)),
        Segment.of(Interval.between( 2, Inclusive,  4, Exclusive, SECONDS), new LinearEquation(Duration.of( 2, SECONDS),  2,  0)),
        Segment.of(Interval.between( 4, Inclusive,  6, Exclusive, SECONDS), new LinearEquation(Duration.of( 4, SECONDS),  2,  1)),
        Segment.of(Interval.between( 6, Inclusive, 12, Exclusive, SECONDS), new LinearEquation(Duration.of( 6, SECONDS),  4,  0)),
        Segment.of(Interval.between(12, Inclusive, 16, Exclusive, SECONDS), new LinearEquation(Duration.of(12, SECONDS),  4, -1)),
        Segment.of(Interval.between(16, Inclusive, 20, Inclusive, SECONDS), new LinearEquation(Duration.of(16, SECONDS),  0,  0))
    );

    final var result = ((LinearProfile) profile::stream).lessThan(other).evaluate(bounds);


    assertIterableEquals(expected, result);
  }
}
