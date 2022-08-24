package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.junit.jupiter.api.Test;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.interval;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class IntervalMapTest {

  @Test
  public void setCoalesce() {
    IntervalMap<String> result = new IntervalMap<String>()
        .set(interval(0, 1, SECONDS), "a")
        .set(interval(1, Exclusive, 2, Exclusive, SECONDS), "a")
        .set(interval(2, 3, SECONDS), "a")

        .set(interval(10, 11, SECONDS), "b")
        .set(interval(9, 12, SECONDS), "b")

        .set(interval(20, 24, SECONDS), "c")
        .set(interval(21, 22, SECONDS), "c");

    IntervalMap<String> expected = new IntervalMap<>(
        Segment.of(interval(0, 3, SECONDS), "a"),
        Segment.of(interval(9, 12, SECONDS), "b"),
        Segment.of(interval(20, 24, SECONDS), "c")
    );

    assertIterableEquals(expected, result);
  }

  @Test
  public void setAllStandard1() {
    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b")
        .set(Interval.between(4, 5, SECONDS), "a") //coalesces successfully, in set!
        .set(Interval.between(8, 10, SECONDS), "a")
        .set(Interval.between(12, 14, SECONDS), "a")
        .set(Interval.between(15, 17, SECONDS), "a");


    IntervalMap<String> right = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(8, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(9, SECONDS), Exclusive, Duration.of(10, SECONDS), Inclusive), "b")
        .set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(15, SECONDS), Exclusive, Duration.of(16, SECONDS), Exclusive), "b");

    left = left.set(right);

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(8, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(8, SECONDS), Inclusive, Duration.of(9, SECONDS), Inclusive), "a")
        .set(Interval.between(Duration.of(9, SECONDS), Exclusive, Duration.of(10, SECONDS), Inclusive), "b")
        .set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(13, SECONDS), Inclusive, Duration.of(14, SECONDS), Inclusive), "a")
        .set(Interval.at(Duration.of(15, SECONDS)), "a")
        .set(Interval.between(Duration.of(15, SECONDS), Exclusive, Duration.of(16, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(16, SECONDS), Inclusive, Duration.of(17, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllStandard2() {
    Interval horizon = Interval.between(0, 23, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "a")
        .set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(10, SECONDS), Exclusive), "b")
        .set(Interval.at(Duration.of(13, SECONDS)), "a")
        .set(Interval.between(14,23, SECONDS), "b");


    IntervalMap<String> right = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.at(Duration.of(0, SECONDS)), "b")
        .set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(7, SECONDS), Exclusive), "a")
        .set(Interval.between(Duration.of(10, SECONDS), Exclusive, Duration.of(11, SECONDS), Inclusive), "a")
        .set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(14, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(15, SECONDS), Inclusive, Duration.of(16, SECONDS), Exclusive), "b")
        .set(Interval.at(Duration.of(17, SECONDS)), "a")
        .set(Interval.between(Duration.of(19, SECONDS), Exclusive, Duration.of(21, SECONDS), Inclusive), "a")
        .set(Interval.at(Duration.of(23, SECONDS)), "a");


    left = left.set(right);

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.at(Duration.of(0, SECONDS)), "b")
        .set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(7, SECONDS), Exclusive), "a")
        .set(Interval.between(Duration.of(7, SECONDS), Inclusive, Duration.of(10, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(10, SECONDS), Exclusive, Duration.of(11, SECONDS), Inclusive), "a")
        .set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(17, SECONDS), Exclusive), "b")
        .set(Interval.at(Duration.of(17, SECONDS)), "a")
        .set(Interval.between(Duration.of(17, SECONDS), Exclusive, Duration.of(19, SECONDS), Inclusive), "b")
        .set(Interval.between(Duration.of(19, SECONDS), Exclusive, Duration.of(21, SECONDS), Inclusive), "a")
        .set(Interval.between(Duration.of(21, SECONDS), Exclusive, Duration.of(23, SECONDS), Exclusive), "b")
        .set(Interval.at(Duration.of(23, SECONDS)), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllAtStartNullEnd() {
    Interval horizon = Interval.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(2, 3, SECONDS), "a")
        .set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    IntervalMap<String> right = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.at(Duration.of(0, SECONDS)), "a")
        .set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    left = left.set(right);

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.at(Duration.of(0, SECONDS)), "a")
        .set(Interval.at(Duration.of(2, SECONDS)), "a")
        .set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllNullStartAtEnd() {
    Interval horizon = Interval.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(2, 3, SECONDS), "a")
        .set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    IntervalMap<String> right = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b")
        .set(Interval.at(Duration.of(4, SECONDS)), "a");


    left = left.set(right);

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.at(Duration.of(2, SECONDS)), "a")
        .set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "b")
        .set(Interval.at(Duration.of(4, SECONDS)), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllCoverWholeHorizon() {
    Interval horizon = Interval.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(2, 3, SECONDS), "a")
        .set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    IntervalMap<String> right = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "a");

    left = left.set(right);

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetInMiddleRange() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(1, 3, SECONDS), "a")
        .set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b")
        .unset(Interval.between(2, 4, SECONDS))
        .unset(Interval.at(Duration.of(5, SECONDS)));

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a")
        .set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b")
        .set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetTouchingStart() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(0, 6, SECONDS), "a")
        .unset(Interval.between(0, 2, SECONDS));

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(6, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetTouchingEnd() {
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(0, 6, SECONDS), "a")
        .unset(Interval.between(5, 6, SECONDS));

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetTouchingAll() {
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(0, 3, SECONDS), "a")
        .set(Interval.between(5, 6, SECONDS), "a")
        .unset(Interval.between(0, 6, SECONDS));

    assertEquals(left.size(), 0);
  }

  @Test
  public void unsetAllTouchingBothEnds() {
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(0, 3, SECONDS), "a")
        .set(Interval.between(4, 6, SECONDS), "a")
        .unset(Interval.between(0, 1, SECONDS))
        .unset(Interval.between(5, 6, SECONDS));

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "a")
        .set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetAllMiniGap(){
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(1, 5, SECONDS), "a")
        .unset(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive))
        .unset(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive));

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a")
        .set(Interval.at(Duration.of(3, SECONDS)), "a")
        .set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetAllSparseInMiddle() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<String>(new IntervalAlgebra(horizon))
        .set(Interval.between(1, 6, SECONDS), "a")
        .unset(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive))
        .unset(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive));

    IntervalMap<String> expected = new IntervalMap<String>(new IntervalAlgebra())
        .set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a")
        .set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "a")
        .set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }
}
