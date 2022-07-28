package gov.nasa.jpl.aerie.constraints.time;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.window;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WindowsTest {
  @Test
  public void addEmpty() {
    final var windows = new Windows();
    windows.add(Window.EMPTY);

    final var expected = new Windows();

    assertEquivalent(expected, windows);
  }

  @Test
  public void addOpenPoint() {
    final var windows = new Windows();
    windows.add(window(1, Exclusive, 1, Exclusive, MICROSECONDS));

    final var expected = new Windows();

    assertEquivalent(expected, windows);
  }

  @Test
  public void addOpenAndClosed() {
    final var windows = new Windows();
    windows.add(window(0, Inclusive, 1, Exclusive, MICROSECONDS));
    windows.add(window(1, Inclusive, 2, Inclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(0, Inclusive,2, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void addOpenAndOpen() {
    final var first = window(0, Inclusive, 1, Exclusive, MICROSECONDS);
    final var second = window(1, Exclusive, 2, Inclusive, MICROSECONDS);
    final var windows = new Windows();
    windows.add(first);
    windows.add(second);

    assertEquivalent(List.of(first, second), windows);
  }

  @Test
  public void addOverlapped() {
    final var windows = new Windows();
    windows.add(window(0,  2, MICROSECONDS));
    windows.add(window(1,  3, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(0,  3, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void addMeeting() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(1,  2, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(0,  2, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void addDoublyOverlapped() {
    final var windows = new Windows();
    // Add two disjoint windows
    windows.add(window(0,  2, MICROSECONDS));
    windows.add(window(3,  5, MICROSECONDS));
    // Then add a window that overlaps both
    windows.add(window(1,  4, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(0,  5, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void addContained() {
    final var windows = new Windows();
    windows.add(window(0,  2, MICROSECONDS));
    windows.add(window(1,  1, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(0,  2, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void addAll() {
    final var windows = new Windows();
    windows.add(window(1,  4, MICROSECONDS));
    windows.add(window(6,  6, MICROSECONDS));

    final var patch = new Windows();
    patch.add(window(0,  2, MICROSECONDS));
    patch.add(window(3,  5, MICROSECONDS));

    windows.addAll(patch);

    final var expected = new Windows();
    expected.add(window(0,  5, MICROSECONDS));
    expected.add(window(6,  6, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractContained() {
    final var windows = new Windows();
    windows.add(window(0,  3, MICROSECONDS));
    windows.subtractPoint(1, MICROSECONDS);

    final var expected = new Windows();
    expected.add(window(0, Inclusive, 1, Exclusive, MICROSECONDS));
    expected.add(window(1, Exclusive, 3, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractOverlappedOnRight() {
    final var windows = new Windows();
    windows.add(window(1,  3, MICROSECONDS));
    windows.subtract(window(2,  Inclusive, 4, Exclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(1, Inclusive, 2, Exclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractOverlappedOnLeft() {
    final var windows = new Windows();
    windows.add(window(1,  3, MICROSECONDS));
    windows.subtract(window(0, Exclusive, 2, Inclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(2, Exclusive, 3, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractDoublyOverlapped() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(2,  4, MICROSECONDS));

    windows.subtract(1,  2, MICROSECONDS);

    final var expected = new Windows();
    expected.add(window(0, Inclusive, 1, Exclusive, MICROSECONDS));
    expected.add(window(2, Exclusive, 4, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractExact() {
    final var windows = new Windows();
    windows.addPoint(-1, MICROSECONDS);
    windows.add(window(0,  3, MICROSECONDS));
    windows.addPoint(7, MICROSECONDS);

    windows.subtract(0,  3, MICROSECONDS);

    final var expected = new Windows();
    expected.addPoint(-1, MICROSECONDS);
    expected.addPoint(7, MICROSECONDS);

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractContaining() {
    final var windows = new Windows();
    windows.addPoint(-2, MICROSECONDS);
    windows.add(window(0, 3, MICROSECONDS));
    windows.addPoint(7, MICROSECONDS);

    windows.subtract(-1,  5, MICROSECONDS);

    final var expected = new Windows();
    expected.addPoint(-2, MICROSECONDS);
    expected.addPoint(7, MICROSECONDS);

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractUnincluded() {
    final var windows = new Windows();
    windows.addPoint(-2, MICROSECONDS);
    windows.addPoint(7, MICROSECONDS);

    windows.subtractPoint(0, MICROSECONDS);

    final var expected = new Windows();
    expected.addPoint(-2, MICROSECONDS);
    expected.addPoint(7, MICROSECONDS);

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractAll() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(2,  4, MICROSECONDS));
    windows.add(window(5,  6, MICROSECONDS));

    final var mask = new Windows();
    mask.add(window(1,  2, MICROSECONDS));
    mask.add(window(4,  5, MICROSECONDS));

    windows.subtractAll(mask);

    final var expected = new Windows();
    expected.add(window(0, Inclusive, 1, Exclusive, MICROSECONDS));
    expected.add(window(2, Exclusive, 4, Exclusive, MICROSECONDS));
    expected.add(window(5, Exclusive, 6, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractPoint() {
    final var windows = new Windows();
    windows.add(window(0, Inclusive, 2, Inclusive, MICROSECONDS));
    windows.subtractPoint(1, MICROSECONDS);

    final var expected = new Windows();
    expected.add(window(0, Inclusive, 1, Exclusive, MICROSECONDS));
    expected.add(window(1, Exclusive, 2, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractAllButPoint() {
    final var windows = new Windows(Window.between(0, Inclusive, 1, Exclusive, MICROSECONDS));
    windows.subtract(Window.between(0, Exclusive, 1, Inclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(Window.at(0, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractOpen() {
    final var windows = new Windows();
    windows.add(window(1,4, MICROSECONDS));
    windows.subtract(window(2, Exclusive, 3, Exclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(1,2, MICROSECONDS));
    expected.add(window(3, 4, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractHalfOpen() {
    final var windows = new Windows();
    windows.add(window(1,4, MICROSECONDS));
    windows.subtract(window(2, Exclusive, 3, Inclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(1, Inclusive, 2, Inclusive, MICROSECONDS));
    expected.add(window(3, Exclusive, 4, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractMeetingOpen() {
    final var windows = new Windows();
    windows.add(window(1, 2, MICROSECONDS));
    windows.subtract(window(2, Exclusive, 3, Exclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(1, 2, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void subtractMetByOpen() {
    final var windows = new Windows();
    windows.add(window(1, 2, MICROSECONDS));
    windows.subtract(window(0, Exclusive, 1, Exclusive, MICROSECONDS));

    final var expected = new Windows();
    expected.add(window(1, 2, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersect() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(2,  4, MICROSECONDS));
    windows.add(window(5,  6, MICROSECONDS));

    windows.intersectWith(1,  3, MICROSECONDS);

    final var expected = new Windows();
    expected.addPoint(1, MICROSECONDS);
    expected.add(window(2,  3, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersectEmpty() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(2,  4, MICROSECONDS));

    windows.intersectWith(Window.EMPTY);

    final var expected = new Windows();

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersectAdjacent() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(2,  4, MICROSECONDS));

    windows.intersectWith(1,  2, MICROSECONDS);

    final var expected = new Windows();
    expected.addPoint(1, MICROSECONDS);
    expected.addPoint(2, MICROSECONDS);

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersectNonintersecting() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(2,  4, MICROSECONDS));

    windows.intersectWith(-10,  -5, MICROSECONDS);

    final var expected = new Windows();

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersectAll() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));
    windows.add(window(2,  4, MICROSECONDS));
    windows.add(window(5,  6, MICROSECONDS));

    final var mask = new Windows();
    mask.addPoint(1, MICROSECONDS);
    windows.intersectWith(1,  3, MICROSECONDS);

    final var expected = new Windows();
    expected.addPoint(1, MICROSECONDS);
    expected.add(window(2,  3, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersectMultiple() {
    final var windows = new Windows();
    windows.add(window(0, 20, MICROSECONDS));

    final var mask = new Windows();
    mask.add(window(0, 5, MICROSECONDS));
    mask.add(window(6, Inclusive,7, Exclusive, MICROSECONDS));
    mask.add(window(7, Exclusive, 8, Inclusive, MICROSECONDS));
    windows.intersectWith(mask);

    final var expected = new Windows();
    expected.add(window(0, 5, MICROSECONDS));
    expected.add(window(6, Inclusive,7, Exclusive, MICROSECONDS));
    expected.add(window(7, Exclusive, 8, Inclusive, MICROSECONDS));

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersectSinglePoint() {
    final var windows = new Windows();
    windows.add(window(0, 1, MICROSECONDS));

    final var mask = new Windows();
    mask.add(window(1, 2, MICROSECONDS));
    windows.intersectWith(mask);

    final var expected = new Windows();
    expected.addPoint(1, MICROSECONDS);

    assertEquivalent(expected, windows);
  }

  @Test
  public void intersectMeeting() {
    final var windows = new Windows();
    windows.add(window(0, Inclusive, 1, Exclusive, MICROSECONDS));

    final var mask = new Windows();
    mask.add(window(1, Inclusive, 2, Inclusive, MICROSECONDS));
    windows.intersectWith(mask);

    final var expected = new Windows();

    assertEquivalent(expected, windows);
  }

  @Test
  public void includesEmpty() {
    final var x = new Windows();

    assertTrue(x.includes(Window.EMPTY));
    assertFalse(x.includesPoint(0, MICROSECONDS));
  }

  @Test
  public void intersectNonintersectingMeeting() {
    final var windows = new Windows();
    windows.add(window(0,  1, MICROSECONDS));

    windows.intersectWith(window(1, Exclusive, 2, Exclusive, MICROSECONDS));

    final var expected = new Windows();

    assertEquivalent(expected, windows);
  }

  @Test
  public void includesWindow() {
    final var x = new Windows();
    x.add(window(-10,  10, MICROSECONDS));

    // included points
    assertTrue(x.includesPoint( 0, MICROSECONDS));
    assertTrue(x.includesPoint(10, MICROSECONDS));
    // sub-intervals
    assertTrue(x.includes(-10,   3, MICROSECONDS));
    assertTrue(x.includes( -2,   3, MICROSECONDS));
    assertTrue(x.includes(  5,  10, MICROSECONDS));
    // exact intervals
    assertTrue(x.includes(-10,  10, MICROSECONDS));

    // excluded points
    assertFalse(x.includesPoint( 15, MICROSECONDS));
    assertFalse(x.includesPoint(-15, MICROSECONDS));
    // overlapping intervals
    assertFalse(x.includes(  5,  15, MICROSECONDS));
    assertFalse(x.includes(-15,  -5, MICROSECONDS));
    // containing intervals
    assertFalse(x.includes(-15,  -15, MICROSECONDS));
  }

  @Test
  public void includesAll() {
    final var x = new Windows();
    x.add(window(-10, 10, MICROSECONDS));
    x.addPoint(15, MICROSECONDS);
    x.addPoint(20, MICROSECONDS);

    final var y = new Windows();
    y.add(window(-10,  -5, MICROSECONDS));
    y.add(window(3,  6, MICROSECONDS));
    y.addPoint(20, MICROSECONDS);

    assertTrue(x.includes(y));
  }

  @Test
  public void includesSelf() {
    final var x = new Windows();
    x.add(window(-10, 10, MICROSECONDS));
    x.addPoint(15, MICROSECONDS);
    x.addPoint(20, MICROSECONDS);

    assertTrue(x.includes(x));
  }

  @Test
  public void asEmptyList() {
    final var windows = new Windows();

    final var windowList = new ArrayList<Window>();
    windows.forEach(windowList::add);

    assertEquals(Collections.emptyList(), windowList);
  }

  @Test
  public void asList() {
    final var windows = new Windows();
    windows.add(window(0,  2, MICROSECONDS));
    windows.add(window(3,  5, MICROSECONDS));
    windows.add(window(1,  4, MICROSECONDS));

    final var windowList = new ArrayList<Window>();
    windows.forEach(windowList::add);

    final var expected = List.of(window(0, 5, MICROSECONDS));

    assertEquals(expected, windowList);
  }

  @Test
  public void intoSpans() {
    final var spans = new Windows(List.of(
        window(0, 2, SECONDS),
        window(1, 3, SECONDS),
        window(5, 5, SECONDS)
    )).intoSpans();

    final var expected = new Spans(
        window(0, 3, SECONDS),
        window(5, 5, SECONDS)
    );

    assertIterableEquals(expected, spans);
  }
}
