package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntervalMapTest {

  @Test
  public void setAllStandard() {
    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra());
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra());

    left.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");
    left.set(Window.between(4, 5, SECONDS), "a"); //coalesces successfully, in set!
    left.set(Window.between(8, 10, SECONDS), "a");
    left.set(Window.between(12, 14, SECONDS), "a");
    left.set(Window.between(15, 17, SECONDS), "a");


    right.set(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(8, SECONDS), Exclusive), "b");
    right.set(Window.between(Duration.of(9, SECONDS), Exclusive, Duration.of(10, SECONDS), Inclusive), "b");
    right.set(Window.between(Duration.of(12, SECONDS), Inclusive, Duration.of(13, SECONDS), Exclusive), "b");
    right.set(Window.between(Duration.of(15, SECONDS), Exclusive, Duration.of(16, SECONDS), Exclusive), "b");


    left.setAll(right);

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");
    expected.set(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Inclusive), "a");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.of(9, SECONDS), Inclusive), "a");
    expected.set(Window.between(Duration.of(9, SECONDS), Exclusive, Duration.of(10, SECONDS), Exclusive), "b");
    expected.set(Window.between(Duration.of(12, SECONDS), Exclusive, Duration.of(13, SECONDS), Exclusive), "a");
    expected.set(Window.between(Duration.of(13, SECONDS), Exclusive, Duration.of(14, SECONDS), Exclusive), "b");
    expected.set(Window.between(Duration.of(15, SECONDS), Exclusive, Duration.of(16, SECONDS), Exclusive), "b");
    expected.set(Window.between(Duration.of(16, SECONDS), Exclusive, Duration.of(17, SECONDS), Exclusive), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void setAllAtStartNullEnd() {
    Window horizon = Window.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(2, 3, SECONDS), "a");
    left.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    right.set(Window.at(Duration.of(0, SECONDS)), "a");
    right.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");


    left.setAll(right);

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.at(Duration.of(0, SECONDS)), "a");
    expected.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "a");
    expected.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "b");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void setAllNullStartAtEnd() {
    Window horizon = Window.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(2, 3, SECONDS), "a");
    left.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    right.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");
    right.set(Window.at(Duration.of(4, SECONDS)), "a");


    left.setAll(right);

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "a");
    expected.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive), "b");
    expected.set(Window.at(Duration.of(4, SECONDS)), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void setAllCoverWholeHorizon() {
    Window horizon = Window.between(0, 4, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(2, 3, SECONDS), "a");
    left.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Exclusive), "b");

    right.set(Window.between(Duration.of(0, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "a");


    left.setAll(right);

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(0, SECONDS), Inclusive, Duration.of(4, SECONDS), Inclusive), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void unsetInMiddleRange() {
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(1, 3, SECONDS), "a");
    left.set(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");

    left.unset(Window.between(2, 4, SECONDS));
    left.unset(Window.at(Duration.of(5, SECONDS)));

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Inclusive), "b");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void unsetTouchingStart() {
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(0, 6, SECONDS), "a");

    left.unset(Window.between(0, 2, SECONDS));

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(6, SECONDS), Inclusive), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void unsetTouchingEnd() {
    Window horizon = Window.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    IntervalMap<String> right = new IntervalMap<>(new Windows.WindowAlgebra(horizon));



    left.set(Window.between(0, 6, SECONDS), "a");

    left.unset(Window.between(5, 6, SECONDS));

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(0, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void unsetTouchingAll() {
    Window horizon = Window.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.between(0, 3, SECONDS), "a");
    left.set(Window.between(5, 6, SECONDS), "a");

    left.unset(Window.between(0, 6, SECONDS));

    for (var i : left.ascendingOrder()) {
      System.out.println(i);
    }

    assertEquals(left.size(), 0);
  }

  @Test
  public void unsetAllTouchingBothEnds() {
    Window horizon = Window.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.between(0, 3, SECONDS), "a");
    left.set(Window.between(4, 6, SECONDS), "a");

    left.unset(Window.between(0, 1, SECONDS));
    left.unset(Window.between(5, 6, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(1, SECONDS), Exclusive, Duration.of(3, SECONDS), Inclusive), "a");
    expected.set(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void unsetAllMiniGap(){
    Window horizon = Window.between(0, 6, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.between(1, 5, SECONDS), "a");

    left.unset(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive));
    left.unset(Window.between(Duration.of(3, SECONDS), Exclusive, Duration.of(4, SECONDS), Inclusive));

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(1, SECONDS), Exclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Window.at(Duration.of(3, SECONDS)), "a");
    expected.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Inclusive), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void unsetAllSparseInMiddle() {
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.between(1, 6, SECONDS), "a");

    left.unset(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive));
    left.unset(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive));

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra());
    expected.set(Window.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Window.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "a");
    expected.set(Window.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Inclusive), "a");

    var leftIter = StreamSupport.stream(left.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(leftIter.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void clearAndIsEmpty() {
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> toTest = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    toTest.set(Window.between(1, 3, SECONDS), "a");
    toTest.set(Window.between(4, 6, SECONDS), "b");

    toTest.clear();

    assertEquals(toTest.size(), 0);
  }

  @Test
  public void getMiddle() {
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.between(1, 3, SECONDS), "b"); //overwrite!
    left.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    left.set(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "b");

    List<Pair<Window, String>> result = left.get(Window.between(2, 5, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    expected.set(Window.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "b");
    expected.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    expected.set(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "a");

    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(result.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void getTouchingStart(){
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.at(Duration.of(0, SECONDS)), "a");
    left.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    left.set(Window.between(Duration.of(4, SECONDS), Inclusive, Duration.of(5, SECONDS), Exclusive), "b");

    List<Pair<Window, String>> result = left.get(Window.between(0, 4, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    expected.set(Window.at(Duration.of(0, SECONDS)), "a");
    expected.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(3, SECONDS), Exclusive), "a");
    expected.set(Window.at(Duration.of(4, SECONDS)), "b");

    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(result.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void getTouchingEnd() {
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.between(Duration.of(2, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "a");
    left.set(Window.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Exclusive), "b");
    left.set(Window.at(Duration.of(7, SECONDS)), "a");

    List<Pair<Window, String>> result = left.get(Window.between(3, 7, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    expected.set(Window.between(Duration.of(3, SECONDS), Inclusive, Duration.of(4, SECONDS), Exclusive), "a");
    expected.set(Window.between(Duration.of(5, SECONDS), Inclusive, Duration.of(6, SECONDS), Exclusive), "b");
    expected.set(Window.at(Duration.of(7, SECONDS)), "a");

    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(result.get(i), expectedIter.get(i));
    }
  }

  @Test
  public void getOverHorizon() {
    Window horizon = Window.between(0, 7, SECONDS);

    IntervalMap<String> left = new IntervalMap<>(new Windows.WindowAlgebra(horizon));

    left.set(Window.at(Duration.of(0, SECONDS)), "a");
    left.set(Window.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    left.set(Window.at(Duration.of(2, SECONDS)), "b");
    left.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "b");
    left.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b");
    left.set(Window.at(Duration.of(2, SECONDS)), "b");
    left.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");
    left.set(Window.at(Duration.of(6, SECONDS)), "a");
    left.set(Window.at(Duration.of(7, SECONDS)), "a");

    List<Pair<Window, String>> result = left.get(Window.between(0, 7, SECONDS));

    IntervalMap<String> expected = new IntervalMap<>(new Windows.WindowAlgebra(horizon));
    expected.set(Window.at(Duration.of(0, SECONDS)), "a");
    expected.set(Window.between(Duration.of(1, SECONDS), Inclusive, Duration.of(2, SECONDS), Exclusive), "a");
    expected.set(Window.at(Duration.of(2, SECONDS)), "b");
    expected.set(Window.between(Duration.of(2, SECONDS), Exclusive, Duration.of(3, SECONDS), Exclusive), "b");
    expected.set(Window.between(Duration.of(4, SECONDS), Exclusive, Duration.of(5, SECONDS), Exclusive), "b");
    expected.set(Window.at(Duration.of(2, SECONDS)), "b");
    expected.set(Window.between(Duration.of(5, SECONDS), Exclusive, Duration.of(6, SECONDS), Exclusive), "b");
    expected.set(Window.at(Duration.of(6, SECONDS)), "a");
    expected.set(Window.at(Duration.of(7, SECONDS)), "a");

    var expectedIter = StreamSupport.stream(expected.ascendingOrder().spliterator(), false).collect(Collectors.toList());
    for (int i = 0; i < expectedIter.size(); i++) {
      assertEquals(result.get(i), expectedIter.get(i));
    }
  }
}
