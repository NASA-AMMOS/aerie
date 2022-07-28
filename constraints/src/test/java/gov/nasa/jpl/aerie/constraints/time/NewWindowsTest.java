package gov.nasa.jpl.aerie.constraints.time;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Exclusive;
import static gov.nasa.jpl.aerie.constraints.time.Window.Inclusivity.Inclusive;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.SECONDS;
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
    assertTrue(windows3.minValidTimePoint().get().getKey().isZero());


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
    NewWindows union = NewWindows.union(orig, addMe);
    orig.addAll(addMe);

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
    System.out.println(before);

    //try all unset variations, with different bound types
    before.unset(Window.between(Duration.of(5, SECONDS), Exclusive,
                                Duration.of(7, SECONDS), Inclusive));
    before.unsetAll(List.of(Window.between(Duration.of(9, SECONDS), Exclusive,
                                           Duration.of(12, SECONDS), Inclusive)));
    before.unsetAll(List.of(Window.between(Duration.of(13, SECONDS), Exclusive,
                                           Duration.of(15, SECONDS), Inclusive)));
    before.unsetAll(Window.between(Duration.of(20, SECONDS), Exclusive,
                                   Duration.of(25, SECONDS), Exclusive),
                    Window.between(Duration.of(26, SECONDS), Exclusive,
                                   Duration.of(27, SECONDS), Exclusive));
    before.unsetAll(new NewWindows(Window.between(29, 30, SECONDS), true));
    before.unsetAll(new NewWindows(Window.between(29, 30, SECONDS), false)); //value doesn't matter

    System.out.println(before);

    //values of before should never have changed!
    assertTrue(before.getListCopy().stream()
                     .map($ -> $.getValue())
                     .reduce(true, Boolean::logicalAnd));

    //check values
    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(5, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(7, SECONDS), Exclusive,
                                                                Duration.of(9, SECONDS), Inclusive), true),
                                         Pair.of(Window.between(Duration.of(12, SECONDS), Exclusive,
                                                                Duration.of(13, SECONDS), Inclusive), true),
                                         Pair.of(Window.between(Duration.of(15, SECONDS), Exclusive,
                                                                Duration.of(20, SECONDS), Inclusive), true),
                                         Pair.of(Window.between(Duration.of(25, SECONDS), Inclusive,
                                                                Duration.of(26, SECONDS), Inclusive), true),
                                         Pair.of(Window.between(Duration.of(27, SECONDS), Inclusive,
                                                                Duration.of(29, SECONDS), Exclusive), true),
                                         Pair.of(Window.between(Duration.of(30, SECONDS), Exclusive,
                                                                Duration.MAX_VALUE, Inclusive), true));

    var beforeAsList = before.getListCopy();
    var expectedAsList = expected.getListCopy();

    for(int i = 0; i < before.size(); i++) {
      assertEquals(expectedAsList.get(i), beforeAsList.get(i));
    }

  }

  @Test
  public void subtractAllMain() {

    //just test truth table associated with the map2 call for this method, don't waste time
    //  with bounds checking as that was done for map2!!

    //orig | new | result
    //  T  |  T  |  F
    //  T  |  F  |  T
    //  T  |  N  |  T
    //  F  |  T  |  F
    //  F  |  F  |  F
    //  F  |  N  |  F
    //  N  |  T  |  N
    //  N  |  F  |  N
    //  N  |  N  |  N

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(11, SECONDS)), false));

    NewWindows subtractMe = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                             Duration.of(2, SECONDS)), true),
                                      Pair.of(Window.between(Duration.of(2, SECONDS), Exclusive,
                                                             Duration.of(3, SECONDS), Inclusive), false),
                                      Pair.of(Window.between(Duration.of(7, SECONDS),
                                                             Duration.of(8, SECONDS)), true),
                                      Pair.of(Window.between(Duration.of(9, SECONDS),
                                                             Duration.of(10, SECONDS)), false),
                                      Pair.of(Window.between(Duration.of(12, SECONDS),
                                                             Duration.of(13, SECONDS)), true),
                                      Pair.of(Window.between(Duration.of(14, SECONDS),
                                                             Duration.of(15, SECONDS)), false));
    NewWindows minus = NewWindows.minus(orig, subtractMe);
    orig.subtractAll(subtractMe);


    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }


    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                                Duration.of(2, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(2, SECONDS), Exclusive,
                                                                Duration.of(4, SECONDS), Inclusive), true),
                                         Pair.of(Window.between(Duration.of(6, SECONDS),
                                                                Duration.of(11, SECONDS)), false));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }
    assertEquals(minus, orig);

  }

  @Test
  public void subtractVariations() {

    //invoke subtractAll(List<Window>), subtractAll(Window... windows), subtract(window), subtract(durations), subtract(point)
    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                            Duration.of(5, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(11, SECONDS)), false));

    orig.subtractAll(List.of(Window.between(Duration.of(0, SECONDS),
                                            Duration.of(2, SECONDS))));
    orig.subtractAll(Window.between(Duration.of(7, SECONDS),
                                    Duration.of(8, SECONDS)));
    orig.subtract(Window.between(Duration.of(13, SECONDS),
                                 Duration.of(14, SECONDS)));
    orig.subtract(3, 4, SECONDS);
    orig.subtractPoint(5, SECONDS);

    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }


    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                                Duration.of(2, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(2, SECONDS), Exclusive,
                                                                Duration.of(3, SECONDS), Exclusive), true),
                                         Pair.of(Window.between(Duration.of(3, SECONDS),
                                                                Duration.of(4, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(4, SECONDS), Exclusive,
                                                                Duration.of(5, SECONDS), Exclusive), true),
                                         Pair.of(Window.at(Duration.of(5, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(6, SECONDS),
                                                                Duration.of(11, SECONDS)), false));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }


    //subtract(window from window), subtract (Window with values)
    var subtractBasic = NewWindows.subtract(Window.between(Duration.of(0, SECONDS),
                                                           Duration.of(10, SECONDS)),
                                            Window.between(Duration.of(5, SECONDS),
                                                           Duration.of(10, SECONDS)));
    assertEquals(subtractBasic.getListCopy().get(0),
                 Pair.of(Window.between(Duration.of(0, SECONDS), Inclusive,
                                        Duration.of(5, SECONDS), Exclusive), true));

    var subtractAdvanced = NewWindows.subtract(Window.between(Duration.of(0, SECONDS),
                                                              Duration.of(10, SECONDS)), false,
                                               Window.between(Duration.of(5, SECONDS),
                                                              Duration.of(10, SECONDS)), true);
    assertEquals(subtractAdvanced.getListCopy().get(0),
                 Pair.of(Window.between(Duration.of(0, SECONDS),
                                        Duration.of(10, SECONDS)), false));


  }

  @Test
  public void intersectWithMain() {

    //just test truth table associated with the map2 call for this method, don't waste time
    //  with bounds checking as that was done for map2!!

    // orig | inFilter | output
    //  T   |    T     |   T
    //  T   |    F     |   F
    //  T   |    N     |   N
    //  F   |    T     |   F
    //  F   |    F     |   F
    //  F   |    N     |   N
    //  N   |    T     |   N
    //  N   |    F     |   N
    //  N   |    N     |   N

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(11, SECONDS)), false));

    NewWindows intersectMe = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                  Duration.of(2, SECONDS)), true),
                                           Pair.of(Window.between(Duration.of(2, SECONDS), Exclusive,
                                                                  Duration.of(3, SECONDS), Inclusive), false),
                                           Pair.of(Window.between(Duration.of(7, SECONDS),
                                                                  Duration.of(8, SECONDS)), true),
                                           Pair.of(Window.between(Duration.of(9, SECONDS),
                                                                  Duration.of(10, SECONDS)), false),
                                           Pair.of(Window.between(Duration.of(12, SECONDS),
                                                                  Duration.of(13, SECONDS)), true),
                                           Pair.of(Window.between(Duration.of(14, SECONDS),
                                                                  Duration.of(15, SECONDS)), false));
    NewWindows intersection = NewWindows.intersection(orig, intersectMe);
    orig.intersectWith(intersectMe);


    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }


    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                                Duration.of(2, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(2, SECONDS), Exclusive,
                                                                Duration.of(3, SECONDS), Inclusive), false),
                                         Pair.of(Window.between(Duration.of(7, SECONDS),
                                                                Duration.of(8, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(9, SECONDS),
                                                                Duration.of(10, SECONDS)), false));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }
    assertEquals(intersection, orig);

  }

  @Test
  public void intersectVariations() {

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(11, SECONDS)), false));

    //test intersectWith (window), intersectwith(duration), intersectwith(window with value),
    //  intersectWith(duration with value), intersection between windows
    orig.intersectWith(Window.between(0, 2, SECONDS));
    orig.intersectWith(7, 8, SECONDS);
    orig.intersectWith(Window.between(Duration.of(2, SECONDS), Exclusive,
                                      Duration.of(3, SECONDS), Inclusive), false);
    orig.intersectWith(10, 12, SECONDS, false);


    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }


    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(1, SECONDS),
                                                                Duration.of(2, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(2, SECONDS), Exclusive,
                                                                Duration.of(3, SECONDS), Inclusive), false),
                                         Pair.of(Window.between(Duration.of(7, SECONDS),
                                                                Duration.of(8, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(9, SECONDS),
                                                                Duration.of(10, SECONDS)), false));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }

  }

  @Test
  public void minMaxTimePoints() {

    //check empty
    NewWindows w = new NewWindows();
    assertFalse(w.minValidTimePoint().isPresent());
    assertFalse(w.maxValidTimePoint().isPresent());
    assertFalse(w.minTrueTimePoint().isPresent());
    assertFalse(w.maxTrueTimePoint().isPresent());

    //check only 1 interval
    w.set(Window.between(Duration.ZERO, Duration.MAX_VALUE), false);
    assertEquals(w.minValidTimePoint().get(), Pair.of(Duration.ZERO, Inclusive));
    assertEquals(w.maxValidTimePoint().get(), Pair.of(Duration.MAX_VALUE, Inclusive));
    assertFalse(w.minTrueTimePoint().isPresent());
    assertFalse(w.maxTrueTimePoint().isPresent());

    //multiple intervals
    w.set(Window.between(Duration.of(3, SECONDS),
                         Duration.of(50, SECONDS)), true);
    w.set(Window.between(75, 200, SECONDS), true);
    assertEquals(w.minValidTimePoint().get(), Pair.of(Duration.ZERO, Inclusive));
    assertEquals(w.maxValidTimePoint().get(), Pair.of(Duration.MAX_VALUE, Inclusive));
    assertEquals(w.minTrueTimePoint().get(), Pair.of(Duration.of(3, SECONDS), Inclusive));
    assertEquals(w.maxTrueTimePoint().get(), Pair.of(Duration.of(200, SECONDS), Inclusive));

    //verify points work too
    w.unset(Window.at(Duration.MAX_VALUE));
    w.set(Window.at(Duration.ZERO), true);
    assertEquals(w.minValidTimePoint().get(), Pair.of(Duration.ZERO, Inclusive));
    assertEquals(w.maxValidTimePoint().get(), Pair.of(Duration.MAX_VALUE, Exclusive));
    assertEquals(w.minTrueTimePoint().get(), Pair.of(Duration.of(0, SECONDS), Inclusive));
    assertEquals(w.maxTrueTimePoint().get(), Pair.of(Duration.of(200, SECONDS), Inclusive));

    //clear and try again
    w.clear();
    assertFalse(w.minValidTimePoint().isPresent());
    assertFalse(w.maxValidTimePoint().isPresent());
    assertFalse(w.minTrueTimePoint().isPresent());
    assertFalse(w.maxTrueTimePoint().isPresent());

  }

  @Test
  public void complement() {
    //the only thing to demonstrate or test here is that it is different from subtracting from forever as that was an
    //  important distinction in implementation.

    NewWindows main = new NewWindows(Pair.of(Window.at(Duration.of(0, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(1, SECONDS),
                                                             Duration.of(3, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(4, SECONDS),
                                                             Duration.of(7, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(8, SECONDS),
                                                             Duration.of(10, SECONDS)), true),
                                     Pair.of(Window.at(Duration.of(12, SECONDS)), true),
                                     Pair.of(Window.at(Duration.of(13, SECONDS)), false),
                                     Pair.of(Window.at(Duration.MAX_VALUE), true));
    main.unset(Window.at(9, SECONDS));

    //if we were to subtract, this is how itd look
    var subtracted = NewWindows.forever();
    subtracted.subtractAll(main);

    //the real complement
    main = main.complement();

    final var mainAsList = main.getListCopy();
    for (final var i : mainAsList) {
      System.out.println(i);
    }

    //check values
    NewWindows expected = new NewWindows(Pair.of(Window.at(0, SECONDS), true),
                                         Pair.of(Window.between(Duration.of(0, SECONDS), Exclusive,
                                                                Duration.of(4, SECONDS), Exclusive), false),
                                         Pair.of(Window.between(Duration.of(4, SECONDS), Inclusive,
                                                                Duration.of(7, SECONDS), Inclusive), true),
                                         Pair.of(Window.between(Duration.of(7, SECONDS), Exclusive,
                                                                Duration.of(13, SECONDS), Exclusive), false),
                                         Pair.of(Window.at(13, SECONDS), true),
                                         Pair.of(Window.between(Duration.of(13, SECONDS), Exclusive,
                                                                Duration.MAX_VALUE, Inclusive), false));
    final var expectedAsList = expected.getListCopy();
    for (int i = 0; i < mainAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), mainAsList.get(i));
    }

    //confirm diff from subtracted
    assertNotEquals(main, subtracted);

  }

  @Test
  public void filterByDurationNormal1() {

    //just test truth table associated with the map2 call for this method, don't waste time
    //  with bounds checking as that was done for map2!!

    // orig | inFilter | output
    //  T   |    T     |   T
    //  T   |    F     |   F
    //  T   |    N     |   N
    //  F   |    T     |   F
    //  F   |    F     |   F
    //  F   |    N     |   N
    //  N   |    T     |   N
    //  N   |    F     |   N
    //  N   |    N     |   N

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(11, SECONDS),
                                                            Duration.of(14, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(16, SECONDS),
                                                            Duration.of(17, SECONDS)), true));


    orig = orig.filterByDuration(Duration.of(3, SECONDS), Duration.of(3, SECONDS));

    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(4, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(6, SECONDS),
                                                                Duration.of(9, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(11, SECONDS),
                                                                Duration.of(14, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(16, SECONDS),
                                                                Duration.of(17, SECONDS)), false));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }

  }

  @Test
  public void filterByDurationNormal2() {


    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(11, SECONDS),
                                                            Duration.of(14, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(16, SECONDS),
                                                            Duration.of(17, SECONDS)), true));


    orig = orig.filterByDuration(Duration.of(1, SECONDS), Duration.of(3, SECONDS));

    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(4, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(6, SECONDS),
                                                                Duration.of(9, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(11, SECONDS),
                                                                Duration.of(14, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(16, SECONDS),
                                                                Duration.of(17, SECONDS)), true));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }

  }

  @Test
  public void filterByDurationZero() {

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(11, SECONDS),
                                                            Duration.of(14, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(16, SECONDS),
                                                            Duration.of(17, SECONDS)), true));


    orig = orig.filterByDuration(Duration.ZERO, Duration.ZERO);

    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(4, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(6, SECONDS),
                                                                Duration.of(9, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(11, SECONDS),
                                                                Duration.of(14, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(16, SECONDS),
                                                                Duration.of(17, SECONDS)), false));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }

  }

  @Test
  public void filterByDurationMinZeroMaxMax() {

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(11, SECONDS),
                                                            Duration.of(14, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(16, SECONDS),
                                                            Duration.of(17, SECONDS)), true));


    orig = orig.filterByDuration(Duration.ZERO, Duration.MAX_VALUE);

    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(4, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(6, SECONDS),
                                                                Duration.of(9, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(11, SECONDS),
                                                                Duration.of(14, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(16, SECONDS),
                                                                Duration.of(17, SECONDS)), true));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }

  }

  @Test
  public void filterByDurationMinMaxMaxMax() {

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(11, SECONDS),
                                                            Duration.of(14, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(16, SECONDS),
                                                            Duration.of(17, SECONDS)), true));


    orig = orig.filterByDuration(Duration.MAX_VALUE, Duration.MAX_VALUE);

    final var origAsList = orig.getListCopy();
    for (final var i : origAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(4, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(6, SECONDS),
                                                                Duration.of(9, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(11, SECONDS),
                                                                Duration.of(14, SECONDS)), false),
                                         Pair.of(Window.between(Duration.of(16, SECONDS),
                                                                Duration.of(17, SECONDS)), false));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < origAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), origAsList.get(i));
    }

  }

  @Test
  public void filterByDurationMinMaxMaxZero() {

    NewWindows orig = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                            Duration.of(4, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(6, SECONDS),
                                                            Duration.of(9, SECONDS)), false),
                                     Pair.of(Window.between(Duration.of(11, SECONDS),
                                                            Duration.of(14, SECONDS)), true),
                                     Pair.of(Window.between(Duration.of(16, SECONDS),
                                                            Duration.of(17, SECONDS)), true));


    try {
      orig = orig.filterByDuration(Duration.MAX_VALUE, Duration.ZERO);
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      assertTrue(e.getMessage().contains("MaxDur +2562047788:00:54.775807 must be greater than MinDur +00:00:00.000000"));
    }

  }

  @Test
  public void removeEndsEmpty() {
    NewWindows empty = new NewWindows();
    NewWindows rf = empty.removeFirst();
    NewWindows rl = empty.removeLast();
    NewWindows rfl = empty.removeFirstAndLast();
    assertEquals(empty, rf);
    assertEquals(rf, rl);
    assertEquals(rl, rfl);
  }

  @Test
  public void removeFirstLastOne() {
    NewWindows one = new NewWindows(Window.between(0, 2, SECONDS), true);
    NewWindows rfl = one.removeFirstAndLast();
    assertEquals(rfl, new NewWindows());
  }

  @Test
  public void removeFirstLastTwo() {
    NewWindows two = new NewWindows(Pair.of(Window.between(0, 2, SECONDS), true),
                                      Pair.of(Window.between(3, 4, SECONDS), true));
    NewWindows rfl = two.removeFirstAndLast();
    assertEquals(rfl, new NewWindows());
  }

  @Test
  public void shiftByStretch() {
    NewWindows orig = new NewWindows(Pair.of(Window.at(0, SECONDS), true),
                                     Pair.of(Window.between(1, 2, SECONDS), false),
                                     Pair.of(Window.between(5, 6, SECONDS), true),
                                     Pair.of(Window.between(7, 8, SECONDS), false),
                                     Pair.of(Window.between(8, Exclusive, 10, Exclusive, SECONDS), true),
                                     Pair.of(Window.between(13, 14, SECONDS), true),
                                     Pair.of(Window.between(14, Exclusive, 16, Exclusive, SECONDS), false),
                                     Pair.of(Window.at(Duration.MAX_VALUE.minus(Duration.of(1, SECONDS))), true)); //long overflow if at max value

    NewWindows result = orig.shiftBy(Duration.of(-1, SECONDS), Duration.of(1, SECONDS));

    final var resultAsList = result.getListCopy();
    for (final var i : resultAsList) {
      System.out.println(i);
    }

    NewWindows expected = new NewWindows(Pair.of(Window.between(Duration.of(0, SECONDS),
                                                                Duration.of(1, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(1, SECONDS), Exclusive,
                                                                Duration.of(2, SECONDS), Inclusive), false),
                                         Pair.of(Window.between(Duration.of(4, SECONDS), Inclusive,
                                                                Duration.of(11, SECONDS), Exclusive), true),
                                         Pair.of(Window.between(Duration.of(12, SECONDS),
                                                                Duration.of(15, SECONDS)), true),
                                         Pair.of(Window.between(Duration.of(15, SECONDS), Exclusive,
                                                                Duration.of(16, SECONDS), Exclusive), false),
                                         Pair.of(Window.between(Duration.MAX_VALUE.minus(Duration.of(2, SECONDS)),
                                                                Duration.MAX_VALUE), true));
    final var expectedAsList = expected.getListCopy();

    for (int i = 0; i < resultAsList.size(); i++) {
      assertEquals(expectedAsList.get(i), resultAsList.get(i));
    }

  }

  @Test
  public void shiftByFromStartFromEndPermsWithInterval() {
    var restrictedAlgebra = new Windows.WindowAlgebra(Window.between(0, 10, SECONDS));

    var leftEnd = Window.between(0, 2, SECONDS);
    var rightEnd = Window.between(8, 10, SECONDS);

    var orig = new NewWindows(restrictedAlgebra);
    orig.setAll(new NewWindows(Pair.of(leftEnd, true), Pair.of(rightEnd, true)));

    var fromStartPosFromEndPos = orig.shiftBy(Duration.of(-1, SECONDS), Duration.of(1, SECONDS));
    assertEquals(fromStartPosFromEndPos, new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true),
                                                        Pair.of(Window.between(7, 10, SECONDS), true)));

    var fromStartPosFromEndNeg = orig.shiftBy(Duration.of(1, SECONDS), Duration.of(-1, SECONDS));
    assertEquals(fromStartPosFromEndNeg, new NewWindows(Pair.of(Window.between(1, 1, SECONDS), true),
                                                        Pair.of(Window.between(9, 9, SECONDS), true)));

    var fromStartNegFromEndPos = orig.shiftBy(Duration.of(1, SECONDS), Duration.of(1, SECONDS));
    assertEquals(fromStartNegFromEndPos, new NewWindows(Pair.of(Window.between(1, 3, SECONDS), true),
                                                        Pair.of(Window.between(9, 10, SECONDS), true)));

    var fromStartNegFromEndNeg = orig.shiftBy(Duration.of(-1, SECONDS), Duration.of(-1, SECONDS));
    assertEquals(fromStartNegFromEndNeg, new NewWindows(Pair.of(Window.between(0, 1, SECONDS), true),
                                                        Pair.of(Window.between(7, 9, SECONDS), true)));

    var removal = orig.shiftBy(Duration.of(0, SECONDS), Duration.of(-3, SECONDS));
    assertEquals(removal, new NewWindows());
  }

  @Test
  public void shiftByFromStartFromEndPermsWithPoint() {
    var restrictedAlgebra = new Windows.WindowAlgebra(Window.between(0, 4, SECONDS));

    var leftEnd = Window.at(0, SECONDS);
    var rightEnd = Window.at(4, SECONDS);

    var orig = new NewWindows(restrictedAlgebra);
    orig.setAll(new NewWindows(Pair.of(leftEnd, true), Pair.of(rightEnd, true)));

    var fromStartPosFromEndPos = orig.shiftBy(Duration.of(-1, SECONDS), Duration.of(1, SECONDS));
    assertEquals(fromStartPosFromEndPos, new NewWindows(Pair.of(Window.between(0, 1, SECONDS), true),
                                                        Pair.of(Window.between(3, 4, SECONDS), true)));

    var fromStartPosFromEndNeg = orig.shiftBy(Duration.of(1, SECONDS), Duration.of(-1, SECONDS));
    assertEquals(fromStartPosFromEndNeg, new NewWindows());

    var fromStartNegFromEndPos = orig.shiftBy(Duration.of(1, SECONDS), Duration.of(1, SECONDS));
    assertEquals(fromStartNegFromEndPos, new NewWindows(Pair.of(Window.between(1, 1, SECONDS), true)));

    var fromStartNegFromEndNeg = orig.shiftBy(Duration.of(-1, SECONDS), Duration.of(-1, SECONDS));
    assertEquals(fromStartNegFromEndPos, new NewWindows(Pair.of(Window.between(1, 1, SECONDS), true)));

    var coalesce = orig.shiftBy(Duration.of(-2, SECONDS), Duration.of(2, SECONDS));
    assertEquals(coalesce, new NewWindows(restrictedAlgebra.bounds(), true));
  }

  @Test
  public void subsetContainedSpillOver() {
    var orig = new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true),
                              Pair.of(Window.between(4, 5, SECONDS), true),
                              Pair.of(Window.between(6, 8, SECONDS), true));

    var spillOver = orig.subsetContained(Window.between(2, 7, SECONDS));

    //if there is spillover, drops those entire intervals - based on implementation of Window.contains
    //ONLY KEEPS IF TRUE!!!
    assertEquals(spillOver, new NewWindows(Pair.of(Window.between(4, 5, SECONDS), true)));

  }

  @Test
  public void subsetContainedMeetsEndLeft() {

    var alg = new Windows.WindowAlgebra(Window.between(0, 10, SECONDS));

    var orig = new NewWindows(alg);
    orig.setAll(new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true),
                               Pair.of(Window.between(4, 5, SECONDS), true),
                               Pair.of(Window.between(6, 10, SECONDS), true)));

    var leftEnd = orig.subsetContained(Window.between(0, 7, SECONDS));

    assertEquals(leftEnd, new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true),
                                      Pair.of(Window.between(4, 5, SECONDS), true)));

  }

  @Test
  public void subsetContainedMeetsEndRight() {
    var alg = new Windows.WindowAlgebra(Window.between(0, 10, SECONDS));

    var orig = new NewWindows(alg);
    orig.setAll(new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true),
                               Pair.of(Window.between(4, 5, SECONDS), true),
                               Pair.of(Window.between(6, 10, SECONDS), true)));

    var rightEnd = orig.subsetContained(Window.between(5, 10, SECONDS));

    assertEquals(rightEnd, new NewWindows(Pair.of(Window.between(6, 10, SECONDS), true)));
  }

  @Test
  public void subsetContainedMeetsEnds() {
    var alg = new Windows.WindowAlgebra(Window.between(0, 10, SECONDS));

    var orig = new NewWindows(alg);
    orig.setAll(new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true),
                               Pair.of(Window.between(4, 5, SECONDS), false),
                               Pair.of(Window.between(6, 10, SECONDS), false)));

    var all = orig.subsetContained(Window.between(0, 10, SECONDS));

    assertEquals(all, new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true))); //only keeps the true ones
  }

  @Test
  public void getOverlapsSpillover() {
    //keeps true or false!
    var orig = new NewWindows(Pair.of(Window.between(0, 3, SECONDS), true),
                              Pair.of(Window.between(4, 5, SECONDS), false),
                              Pair.of(Window.between(6, 8, SECONDS), false));

    var spillOver = orig.getOverlaps(Window.between(2, Exclusive, 7, Exclusive, SECONDS));

    assertEquals(spillOver, new NewWindows(Pair.of(Window.between(2, Exclusive, 3, Inclusive, SECONDS), true),
                                           Pair.of(Window.between(4, 5, SECONDS), false),
                                           Pair.of(Window.between(6, Inclusive, 7, Exclusive, SECONDS), false)));
  }

  @Test
  public void isNotNullMain() {

    //same test scheme as includes below, just with different evaluations as we just check null, not values.

    // orig  |  other   | output
    //  T    |    T     |   T
    //  T    |    F     |   T
    //  T    |    N     |   T
    //  F    |    T     |   T
    //  F    |    F     |   T
    //  F    |    N     |   T
    //  N    |    T     |   F
    //  N    |    F     |   F
    //  N    |    N     |   T

    var nw = new NewWindows(new Windows.WindowAlgebra(Window.between(1, 2, SECONDS)));

    nw.set(Window.between(1, 2, SECONDS), true);
    assertTrue(nw.isNotNull(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), true)))); //TT case
    assertTrue(nw.isNotNull(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), false)))); //TF case
    assertTrue(nw.isNotNull(new NewWindows())); //TN case

    nw.set(Window.between(1, 2, SECONDS), false);
    assertTrue(nw.isNotNull(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), true)))); //FT case
    assertTrue(nw.isNotNull(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), false)))); //FF case
    assertTrue(nw.isNotNull(new NewWindows())); //FN case

    nw.clear();
    assertFalse(nw.isNotNull(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), true)))); //NT case
    assertFalse(nw.isNotNull(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), false)))); //NF case
    assertTrue(nw.isNotNull(new NewWindows())); //NN case

  }

  @Test
  public void isNotNullVariations() {

    //want to test out isNotNull(Window), isNotNull(Duration), isNotNull(Point) - do all true for now.
    var nw = new NewWindows(new Windows.WindowAlgebra(Window.between(1, 2, SECONDS)));

    nw.set(Window.between(1, 2, SECONDS), true);
    assertTrue(nw.isNotNull(Window.between(1, 2, SECONDS), false)); //values dont matter, that was already tested.
    assertTrue(nw.isNotNull(1, 2, SECONDS, false));
    assertTrue(nw.pointIsNotNull(1, SECONDS, false));

  }

  @Test
  public void includesMain() {

    //just test truth table associated with the map2 call for this method, don't waste time
    //  with bounds checking as that was done for map2!!

    // orig  |  other   | output
    //  T    |    T     |   T
    //  T    |    F     |   T
    //  T    |    N     |   T
    //  F    |    T     |   F
    //  F    |    F     |   T
    //  F    |    N     |   T
    //  N    |    T     |   F
    //  N    |    F     |   T
    //  N    |    N     |   T

    var nw = new NewWindows(new Windows.WindowAlgebra(Window.between(1, 2, SECONDS)));

    nw.set(Window.between(1, 2, SECONDS), true);
    assertTrue(nw.includes(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), true)))); //TT case
    assertTrue(nw.includes(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), false)))); //TF case
    assertTrue(nw.includes(new NewWindows())); //TN case

    nw.set(Window.between(1, 2, SECONDS), false);
    assertFalse(nw.includes(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), true)))); //FT case
    assertTrue(nw.includes(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), false)))); //FF case
    assertTrue(nw.includes(new NewWindows())); //FN case

    nw.clear();
    assertFalse(nw.includes(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), true)))); //NT case
    assertTrue(nw.includes(new NewWindows(Pair.of(Window.between(1, 2, SECONDS), false)))); //NF case
    assertTrue(nw.includes(new NewWindows())); //NN case

  }

  @Test
  public void includesVariations() {

    //want to test out includes(Window), includes(Duration), includes(Point) - do all true for now.
    var nw = new NewWindows(new Windows.WindowAlgebra(Window.between(1, 2, SECONDS)));

    nw.set(Window.between(1, 2, SECONDS), true);
    assertTrue(nw.includes(Window.between(1, 2, SECONDS)));
    assertTrue(nw.includes(1, 2, SECONDS));
    assertTrue(nw.includesPoint(1, SECONDS));

  }

  @Test
  public void iterator() {
    var nw = new NewWindows(new Windows.WindowAlgebra(Window.between(1, 2, SECONDS)));
    nw.set(Window.between(1, 2, SECONDS), true);

    //some simple iterator tests
    var iter = nw.iterator();
    iter.forEachRemaining($ -> assertEquals($, Pair.of(Window.between(1, 2, SECONDS), true)));

    iter = nw.iterator(); //everything has been consumed - need to reset
    assertEquals(iter.next(), Pair.of(Window.between(1, 2, SECONDS), true));
    assertFalse(iter.hasNext());
  }
}
