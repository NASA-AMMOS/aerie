package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Interval.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntervalMapTest {

  @Test
  public void setAllStandard1() {
    Interval horizon = Interval.between(0, 17, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra());

    left.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");
    left.set(Interval.between(4, 5, SECONDS), "a"); //coalesces successfully, in set!
    left.set(Interval.between(8, 10, SECONDS), "a");
    left.set(Interval.between(12, 14, SECONDS), "a");
    left.set(Interval.between(15, 17, SECONDS), "a");


    right.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(8, SECONDS), Exclusive), "b");
    right.set(Interval.between(Duration.of(9, SECONDS), Exclusive, Duration.of(10, SECONDS), Inclusive), "b");
    right.set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "b");
    right.set(Interval.between(Duration.of(15, SECONDS), Exclusive, Duration.of(16, SECONDS), Exclusive), "b");


    left.setAll(right);

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(8, SECONDS), Exclusive), "b");
    expected.set(Interval.between(Duration.of(8, SECONDS), Inclusive, Duration.of(9, SECONDS), Inclusive), "a");
    expected.set(Interval.between(Duration.of(9, SECONDS), Exclusive, Duration.of(10, SECONDS), Inclusive), "b");
    expected.set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "b");
    expected.set(Interval.between(Duration.of(13, SECONDS), Inclusive, Duration.of(14, SECONDS), Inclusive), "a");
    expected.set(Interval.at(Duration.of(15, SECONDS)), "a");
    expected.set(Interval.between(Duration.of(15, SECONDS), Exclusive, Duration.of(16, SECONDS), Exclusive), "b");
    expected.set(Interval.between(Duration.of(16, SECONDS), Inclusive, Duration.of(17, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllStandard2() {
    Interval horizon = Interval.between(0, 23, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(3, SECONDS), Inclusive), "a");
    left.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(10, SECONDS), Exclusive), "b");
    left.set(Interval.at(Duration.of(13, SECONDS)), "a");
    left.set(Interval.between(14,23, SECONDS), "b");


    right.set(Interval.at(Duration.of(0, SECONDS)), "b");
    right.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(7, SECONDS), Exclusive), "a");
    right.set(Interval.between(Duration.of(10, SECONDS), Exclusive, Duration.of(11, SECONDS), Inclusive), "a");
    right.set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(14, SECONDS), Exclusive), "b");
    right.set(Interval.between(Duration.of(15, SECONDS), Inclusive, Duration.of(16, SECONDS), Exclusive), "b");
    right.set(Interval.at(Duration.of(17, SECONDS)), "a");
    right.set(Interval.between(Duration.of(19, SECONDS), Exclusive, Duration.of(21, SECONDS), Inclusive), "a");
    right.set(Interval.at(Duration.of(23, SECONDS)), "a");


    left.setAll(right);

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.at(Duration.of(0, SECONDS)), "b");
    expected.set(Interval.between(Duration.of(0, SECONDS), Exclusive, Duration.of(7, SECONDS), Exclusive), "a");
    expected.set(Interval.between(Duration.of(7, SECONDS), Inclusive, Duration.of(10, SECONDS), Exclusive), "b");
    expected.set(Interval.between(Duration.of(10, SECONDS), Exclusive, Duration.of(11, SECONDS), Inclusive), "a");
    expected.set(Interval.between(Duration.of(12, SECONDS), Inclusive, Duration.of(17, SECONDS), Exclusive), "b");
    expected.set(Interval.at(Duration.of(17, SECONDS)), "a");
    expected.set(Interval.between(Duration.of(17, SECONDS), Exclusive, Duration.of(19, SECONDS), Inclusive), "b");
    expected.set(Interval.between(Duration.of(19, SECONDS), Exclusive, Duration.of(21, SECONDS), Inclusive), "a");
    expected.set(Interval.between(Duration.of(21, SECONDS), Exclusive, Duration.of(23, SECONDS), Exclusive), "b");
    expected.set(Interval.at(Duration.of(23, SECONDS)), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllAtStartNullEnd() {
    Interval horizon = Interval.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));



    left.set(Interval.between(2, 3, SECONDS), "a");
    left.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    right.set(Interval.at(Duration.of(0, SECONDS)), "a");
    right.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");


    left.setAll(right);

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.at(Duration.of(0, SECONDS)), "a");
    expected.set(Interval.at(Duration.of(2, SECONDS)), "a");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllNullStartAtEnd() {
    Interval horizon = Interval.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));



    left.set(Interval.between(2, 3, SECONDS), "a");
    left.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    right.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");
    right.set(Interval.at(Duration.of(4, SECONDS)), "a");


    left.setAll(right);

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.at(Duration.of(2, SECONDS)), "a");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "b");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void setAllCoverWholeHorizon() {
    Interval horizon = Interval.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));



    left.set(Interval.between(2, 3, SECONDS), "a");
    left.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    right.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "a");


    left.setAll(right);

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetInMiddleRange() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(1, 3, SECONDS), "a");
    left.set(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");

    left.unset(Interval.between(2, 4, SECONDS));
    left.unset(Interval.at(Duration.of(5, SECONDS)));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetTouchingStart() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 6, SECONDS), "a");

    left.unset(Interval.between(0, 2, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(6, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetTouchingEnd() {
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new IntervalAlgebra(horizon));



    left.set(Interval.between(0, 6, SECONDS), "a");

    left.unset(Interval.between(5, 6, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(0, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetTouchingAll() {
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 3, SECONDS), "a");
    left.set(Interval.between(5, 6, SECONDS), "a");

    left.unset(Interval.between(0, 6, SECONDS));

    assertEquals(left.size(), 0);
  }

  @Test
  public void unsetAllTouchingBothEnds() {
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(0, 3, SECONDS), "a");
    left.set(Interval.between(4, 6, SECONDS), "a");

    left.unset(Interval.between(0, 1, SECONDS));
    left.unset(Interval.between(5, 6, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "a");
    expected.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetAllMiniGap(){
    Interval horizon = Interval.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(1, 5, SECONDS), "a");

    left.unset(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive));
    left.unset(Interval.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Interval.at(Duration.of(3, SECONDS)), "a");
    expected.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void unsetAllSparseInMiddle() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(1, 6, SECONDS), "a");

    left.unset(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive));
    left.unset(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra());
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "a");
    expected.set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Inclusive), "a");

    assertIterableEquals(expected, left);
  }

  @Test
  public void clearAndIsEmpty() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> toTest = new IntervalMap<>(new IntervalAlgebra(horizon));
    toTest.set(Interval.between(1, 3, SECONDS), "a");
    toTest.set(Interval.between(4, 6, SECONDS), "b");

    toTest.clear();

    assertTrue(toTest.isEmpty());
  }

  @Test
  public void getMiddle() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(1, 3, SECONDS), "b");
    left.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    left.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "b");

    List<Pair<Interval, String>> result = left.get(Interval.between(2, 5, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra(horizon));
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    expected.set(Interval.at(Duration.of(3, SECONDS)), "b");
    expected.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "b");

    assertIterableEquals(expected, result);
  }

  @Test
  public void getTouchingStart(){
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.at(Duration.of(0, SECONDS)), "a");
    left.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    left.set(Interval.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "b");

    List<Pair<Interval, String>> result = left.get(Interval.between(0, 4, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra(horizon));
    expected.set(Interval.at(Duration.of(0, SECONDS)), "a");
    expected.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    expected.set(Interval.at(Duration.of(4, SECONDS)), "b");

    assertIterableEquals(expected, result);
  }

  @Test
  public void getTouchingEnd() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.between(Duration.of(2, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "a");
    left.set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Exclusive), "b");
    left.set(Interval.at(Duration.of(7, SECONDS)), "a");

    List<Pair<Interval, String>> result = left.get(Interval.between(3, 7, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra(horizon));
    expected.set(Interval.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "a");
    expected.set(Interval.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Exclusive), "b");
    expected.set(Interval.at(Duration.of(7, SECONDS)), "a");

    assertIterableEquals(expected, result);
  }

  @Test
  public void getOverHorizon() {
    Interval horizon = Interval.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new IntervalAlgebra(horizon));

    left.set(Interval.at(Duration.of(0, SECONDS)), "a");
    left.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    left.set(Interval.at(Duration.of(2, SECONDS)), "b");
    left.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "b");
    left.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b");
    left.set(Interval.at(Duration.of(2, SECONDS)), "b");
    left.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");
    left.set(Interval.at(Duration.of(6, SECONDS)), "a");
    left.set(Interval.at(Duration.of(7, SECONDS)), "a");

    List<Pair<Interval, String>> result = left.get(Interval.between(0, 7, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new IntervalAlgebra(horizon));
    expected.set(Interval.at(Duration.of(0, SECONDS)), "a");
    expected.set(Interval.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Interval.at(Duration.of(2, SECONDS)), "b");
    expected.set(Interval.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "b");
    expected.set(Interval.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b");
    expected.set(Interval.at(Duration.of(2, SECONDS)), "b");
    expected.set(Interval.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");
    expected.set(Interval.at(Duration.of(6, SECONDS)), "a");
    expected.set(Interval.at(Duration.of(7, SECONDS)), "a");

    assertIterableEquals(expected, result);
  }
}
