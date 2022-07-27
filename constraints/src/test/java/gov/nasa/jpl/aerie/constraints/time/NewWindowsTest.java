package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.nasa.jpl.aerie.constraints.Assertions.assertEquivalent;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.window;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static org.junit.jupiter.api.Assertions.*;

public class NewWindowsTest {

  @Test
  public void constructorTests() {

    //just verify the constructors work right
    var windows1 = new NewWindows();
    var windows2 = new NewWindows(windows1);
    assertTrue(windows1.equals(windows2));
    var windows3 = new NewWindows(List.of(Pair.of(Window.between(Duration.MIN_VALUE,
                                                                 Duration.of(0, SECONDS)),true),
                                          Pair.of(Window.at(1, SECONDS), true),
                                          Pair.of(Window.between(Duration.of(2, SECONDS),
                                                                 Duration.of(3, SECONDS)),false),
                                          Pair.of(Window.at(3, SECONDS), true)));
    var windows4 = new NewWindows(Pair.of(Window.between(Duration.MIN_VALUE,
                                                         Duration.of(0, SECONDS)),true),
                                  Pair.of(Window.at(1, SECONDS), true),
                                  Pair.of(Window.between(Duration.of(2, SECONDS),
                                                                 Duration.of(3, SECONDS)),false),
                                  Pair.of(Window.at(3, SECONDS), true));


    for (var i : windows3.getListCopy()) {
      System.out.println(i);
    }
    assertTrue(windows3.minTimePoint().get().isZero());


    windows2 = new NewWindows(windows3);
    assertTrue(windows2.equals(windows3));
    assertTrue(windows3.equals(windows4));


    var windows5 = NewWindows.defaultTrueWindows(
                                                      Window.between(Duration.MIN_VALUE,
                                                                     Duration.of(0, SECONDS)),
                                                      Window.at(1, SECONDS),
                                                      Window.between(Duration.of(2, SECONDS),
                                                                     Duration.of(3, SECONDS)),
                                                      Window.at(3, SECONDS));
    var windows6 = NewWindows.defaultTrueWindows(List.of(Window.between(Duration.MIN_VALUE,
                                                                        Duration.of(0, SECONDS)),
                                                         Window.at(1, SECONDS),
                                                         Window.between(Duration.of(2, SECONDS),
                                                                        Duration.of(3, SECONDS)),
                                                         Window.at(3, SECONDS)));


    assertTrue(windows5.equals(windows6));
    var windows7 = new NewWindows(Window.between(3, 4, SECONDS), false);

  }

  @Test
  public void addAllMain() {

    //just test truth table associated with the map2 call for this method, don't waste time
    //  with bounds checking as that was done for map2!!

    //orig | new | result
    //  T  |  T  |  T
    //  T  |  F  |  T
    //  T  |  N  |  T
    //  F  |  T  |  T
    //  F  |  F  |  F
    //  F  |  N  |  F
    //  N  |  T  |  T
    //  N  |  F  |  F
    //  N  |  N  |  N

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(2, SECONDS),
                                                            Duration.of(5, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false));

    NewWindows addMe = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                             Duration.of(3, SECONDS)), true),
                                      Pair.of(Window.between(Duration.of(4, SECONDS),
                                                             Duration.of(7, SECONDS)), false),
                                      Pair.of(Window.between(Duration.of(8, SECONDS),
                                                             Duration.of(10, SECONDS)), true));
    orig.addAll(addMe);
    NewWindows union = NewWindows.union(orig, addMe);

    final var origAsList = orig.getListCopy();

    for (final var i : origAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(5, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(5, SECONDS), Exclusive,
                                                                Duration.of(8, SECONDS), Exclusive), false),
                                         Pair.of(Window.between(Duration.of(8, SECONDS),
                                                                Duration.of(10, SECONDS)), true));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }
    assertEquals(union, orig);

  }

  @Test
  public void addVariations() {

    //invoke add(valueless), add(with value), addPoint(valueless), addPoint(with value)
    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(2, SECONDS),
                                                            Duration.of(5, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false));

    orig.addPoint(0, SECONDS);
    orig.add(Window.between(Duration.of(0, SECONDS), Exclusive,
                            Duration.of(3, SECONDS), Inclusive));
    orig.addPoint(4, SECONDS, false);
    orig.add(Window.between(Duration.of(4, SECONDS), Exclusive,
                            Duration.of(7, SECONDS), Inclusive),
             false);
    orig.add(Window.between(Duration.of(8, SECONDS),
                            Duration.of(10, SECONDS)),
             true);

    final var origAsList = orig.getListCopy();

    for (final var i : origAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(5, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(5, SECONDS), Exclusive,
                                                                Duration.of(8, SECONDS), Exclusive), false),
                                         Pair.of(Window.between(Duration.of(8, SECONDS),
                                                                Duration.of(10, SECONDS)), true));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }

  }

  @Test
  public void setAndSetAll() {

    NewWindows w = new NewWindows();

    w.set(Window.between(Duration.ZERO, Duration.MAX_VALUE), false);

    //added correctly?
    assertEquals(w.size(), 1);

    //add at front, back, in between, make sure they're added correctly
    w.set(Window.at(Duration.ZERO), true); //don't coalesce
    w.set(Window.at(Duration.MAX_VALUE), false); //coalesce implictly, nothing happens w/ this line
    w.set(Window.between(Duration.of(3, SECONDS),
                         Duration.of(50, SECONDS)), true);

    //added correctly?
    assertEquals(w.size(), 4);


    //do a setAll
    NewWindows nw = new NewWindows();
    nw.setAll(w);

    assertEquals(w, nw);

  }

  @Test
  public void isEmptyVsIsFalse() {

    //create a window of just true blocks
    NewWindows soTrue = new NewWindows(Pair.of(Window.between(Duration.of(3, SECONDS),
                                                              Duration.of(5, SECONDS)), true),
                                       Pair.of(Window.between(Duration.of(7, SECONDS),
                                                              Duration.of(10, SECONDS)), true),
                                       Pair.of(Window.between(Duration.of(12, SECONDS),
                                                              Duration.of(15, SECONDS)), true),
                                       Pair.of(Window.between(Duration.of(30, SECONDS),
                                                              Duration.of(35, SECONDS)), true));

    //check isFalse, verify the return value is false as either true or null
    //isEmpty is different from isFalse. isEmpty checks is there anything at all - all false would still return false
    assertFalse(soTrue.isFalse());
    assertFalse(soTrue.isEmpty());

    //subtract forever from it
    soTrue.subtractAll(NewWindows.forever());

    //check isFalse, verify the return value is true as everything should be set to false
    //here isFalse and isEmpty diverge
    assertTrue(soTrue.isFalse());
    assertFalse(soTrue.isEmpty());

    //clear it out, verify cleared
    soTrue.clear();
    assertEquals(soTrue.size(), 0);

    //check isFalse and isEmpty - the latter is true as nothing is in the list
    //isFalse is vacuously true as nothing is in it so I guess its false, not a well defined operation
    //when checking isFalse for meaning, therefore, check isEmpty as well!
    assertTrue(soTrue.isFalse());
    assertTrue(soTrue.isEmpty());

  }

  @Test
  public void unsetMain() {
    NewWindows before = NewWindows.forever();

    before.unset();
    before.unsetAll();
    before.unset();

    NewWindows expected = new NewWindows();

    var beforeAsList = before.getListCopy();
    var afterAsList = expected.getListCopy();

    for(int i = 0; i < expected.size(); i++) {
      assertEquals(afterAsList.get(i), beforeAsList.get(i));
    }

  }

  @Test
  public void unsetVariations() {

  }

  @Test
  public void subtractAllMain() {

  }

  @Test
  public void subtractVariations() {

  }

  @Test
  public void intersectWithMain() {

  }

  @Test
  public void intersectVariations() {

  }

  @Test
  public void minMaxTimePoints() {

  }

  @Test
  public void complement() {

  }

  @Test
  public void filterByDurationNormal() {

  }

  @Test
  public void filterByDurationExtrema() {

  }

  @Test
  public void removeEndsEmpty() {

  }

  @Test
  public void removeFirstLastOne() {

  }

  @Test
  public void shiftByStretch() {

  }

  @Test
  public void shiftByFromStartFromEndPermsWithInterval() {

  }

  @Test
  public void shiftByFromStartFromEndPermsWithPoint() {

  }

  @Test
  public void subsetContainedSpillOver() {

  }

  @Test
  public void subsetContainedMeetsEndLeft() {

  }

  @Test
  public void subsetContainedMeetsEndRight() {

  }

  @Test
  public void subsetContainedMeetsEnds() {

  }

  @Test
  public void getOverlapsSpllover() {

  }

  @Test
  public void isNotNullMain() {

  }

  @Test
  public void isNotNullVariations() {

  }

  @Test
  public void includesMain() {

  }

  @Test
  public void includesVariations(){

  }

  @Test
  public void iterator() {

  }
}
