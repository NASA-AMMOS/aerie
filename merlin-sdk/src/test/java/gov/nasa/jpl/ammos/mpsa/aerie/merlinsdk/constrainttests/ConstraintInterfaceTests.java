package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constrainttests;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintStructure;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConstraintInterfaceTests {

  // This test tests how constraints are "anded" together.
  // The logic tested is contained in the Windows::intersectWith method, which already has tests in WindowsTest.java
  // expected result: [6,8], [11,15], [21,25], [27,27]
  @Test
  public void And() {
    Constraint res = ConstraintTestSetup.c1.and(ConstraintTestSetup.c2);
    Windows windows = res.getWindows();

    //check that non-overlapping instances are removed (before and after overlapping windows)
    assertFalse(windows.includes(0, Duration.SECONDS, 3, Duration.SECONDS));
    assertFalse(windows.includes(27, Duration.SECONDS, 29, Duration.SECONDS));

    //check that windows that overlap are clipped (before and after overlap)
    assertTrue(windows.includes(6, Duration.SECONDS, 8, Duration.SECONDS));
    assertFalse(windows.includes(5, Duration.SECONDS, 8, Duration.SECONDS));
    assertTrue(windows.includes(21, Duration.SECONDS, 25, Duration.SECONDS));
    assertTrue(windows.includes(27, Duration.SECONDS, 27, Duration.SECONDS));

    //check that duplicate windows are captured and windows contain only expected results
    assertTrue(windows.includes(11, Duration.SECONDS, 15, Duration.SECONDS));
    Windows expected = new Windows();
    expected.add(6, Duration.SECONDS, 8, Duration.SECONDS);
    expected.add(11, Duration.SECONDS, 15, Duration.SECONDS);
    expected.add(21, Duration.SECONDS, 25, Duration.SECONDS);
    expected.add(27, Duration.SECONDS, 27, Duration.SECONDS);
    assertEquals(windows, expected);
  }

  // This test tests how constraints are "or-ed" together.
  // The logic tested is contained in the Windows::addAll method, which already has tests in WindowsTest.java
  // expected result: [0,3], [5,10], [11,15], [17,18], [20,29]
  @Test
  public void Or() {
    Constraint res = ConstraintTestSetup.c1.or(ConstraintTestSetup.c2);
    var windows = res.getWindows();

    //check that non-overlapping windows remain intact
    assertTrue(windows.includes(0, Duration.SECONDS, 3, Duration.SECONDS));
    assertTrue(windows.includes(17, Duration.SECONDS, 18, Duration.SECONDS));

    //check that overlapping windows collapsed
    assertTrue(windows.includes(5, Duration.SECONDS, 10, Duration.SECONDS));
    assertTrue(windows.includes(11, Duration.SECONDS, 15, Duration.SECONDS));
    assertTrue(windows.includes(20, Duration.SECONDS, 29, Duration.SECONDS));

    //check that windows contain only expected results
    Windows expected = new Windows();
    expected.add(0, Duration.SECONDS, 3, Duration.SECONDS);
    expected.add(0, Duration.SECONDS, 3, Duration.SECONDS);
    expected.add(5, Duration.SECONDS, 10, Duration.SECONDS);
    expected.add(11, Duration.SECONDS, 15, Duration.SECONDS);
    expected.add(17, Duration.SECONDS, 18, Duration.SECONDS);
    expected.add(20, Duration.SECONDS, 29, Duration.SECONDS);
    assertEquals(windows, expected);
  }

  //Windows: [0,3], [5,8], [11,15], [20,25], [27,29]
  //Windows: [6,10], [11,15], [17,18], [21,27]
  // This test tests a constraint that is subtracted from another constraint
  // The logic tested is contained in the Windows::subtractAll method, which already has tests in WindowsTest.java
  // expected result: [0,3], [5,10], [11,15], [17,18], [20,29]
  @Test
  public void Minus() {
    Constraint res = ConstraintTestSetup.c1.minus(ConstraintTestSetup.c2);
    var windows = res.getWindows();

    //check that windows not overlapped by or contained by windows in constraint subtracted from this constraint remain
    assertTrue(windows.includes(0, Duration.SECONDS, 3, Duration.SECONDS));

    //check that windows overlapped or contained by windows in constraint subctracted from this constraint are removed
    assertTrue(windows.includes(5, Duration.SECONDS, 5999999, Duration.MICROSECONDS));
    assertTrue(windows.includes(20, Duration.SECONDS, 20999999, Duration.MICROSECONDS));
    assertTrue(windows.includes(27000001, Duration.MICROSECONDS, 29, Duration.SECONDS));

    //check that windows contain only expected results
    Windows expected = new Windows();
    expected.add(0, Duration.SECONDS, 3, Duration.SECONDS);
    expected.add(5, Duration.SECONDS, 5999999, Duration.MICROSECONDS);
    expected.add(20, Duration.SECONDS, 20999999, Duration.MICROSECONDS);
    expected.add(27000001, Duration.MICROSECONDS, 29, Duration.SECONDS);
    assertEquals(windows, expected);
  }

  public void basicWindowsATest(Windows windows) {
    assertTrue(windows.includes(0, Duration.SECONDS, 3, Duration.SECONDS));
    Windows expected = new Windows();
    expected.add(0, Duration.SECONDS, 3, Duration.SECONDS);
    assertEquals(windows, expected);
  }

  public void checkStateSet(Constraint constraint) {
    assertTrue(constraint.getStateIds().contains(ConstraintTestSetup.stateA));
    assertTrue(constraint.getStateIds().contains(ConstraintTestSetup.stateB));
    assertTrue(constraint.getStateIds().contains(ConstraintTestSetup.stateC));
    assertTrue(constraint.getStateIds().contains(ConstraintTestSetup.stateD));
  }

  public void checkActivitySet(Constraint constraint) {
    assertTrue(constraint.getActivityIds().contains(ConstraintTestSetup.actA));
    assertTrue(constraint.getActivityIds().contains(ConstraintTestSetup.actB));
    assertTrue(constraint.getActivityIds().contains(ConstraintTestSetup.actC));
    assertTrue(constraint.getActivityIds().contains(ConstraintTestSetup.actD));
  }

  @Test
  public void ConstructorUsingAState() {
    var constraint = Constraint.createStateConstraint(ConstraintTestSetup.stateA, () -> new Windows(
        ConstraintTestSetup.a), ConstraintStructure.ofStateConstraint(null, null, null));
    var windows = constraint.getWindows();

    assertTrue(constraint.getStateIds().contains(ConstraintTestSetup.stateA));
    basicWindowsATest(windows);
  }

  @Test
  public void ConstructorUsingAStateSet() {
    var constraint = Constraint.createStateConstraint(ConstraintTestSetup.stateIDs, () -> new Windows(
        ConstraintTestSetup.a), ConstraintStructure.ofStateConstraint(null, null, null));
    var windows = constraint.getWindows();

    checkStateSet(constraint);
    basicWindowsATest(windows);
  }

  @Test
  public void ConstructorUsingAnActivity() {
    var constraint = Constraint.createActivityConstraint(ConstraintTestSetup.actA, () -> new Windows(
        ConstraintTestSetup.a), ConstraintStructure.ofActivityConstraint(null, null));
    var windows = constraint.getWindows();

    assertTrue(constraint.getActivityIds().contains(ConstraintTestSetup.actA));
    basicWindowsATest(windows);
  }

  @Test
  public void ConstructorUsingAnActivitySet() {
    var constraint = Constraint.createActivityConstraint(ConstraintTestSetup.activityIDs, () -> new Windows(
        ConstraintTestSetup.a), ConstraintStructure.ofActivityConstraint(null, null));
    var windows = constraint.getWindows();

    checkActivitySet(constraint);
    basicWindowsATest(windows);
  }

  @Test
  public void ConstructorUsingStateAndActivity() {
    var constraint = Constraint.create(
        null,
        ConstraintTestSetup.activityIDs,
        ConstraintTestSetup.stateIDs,
        () -> new Windows(
            ConstraintTestSetup.a),
        null);
    checkActivitySet(constraint);
    checkStateSet(constraint);
  }

  @Test
  public void Combine() {
    Set<String> state1 = Set.of(
        ConstraintTestSetup.stateA,
        ConstraintTestSetup.stateB,
        ConstraintTestSetup.stateC);
    Set<String> state2 = Set.of(ConstraintTestSetup.stateC, ConstraintTestSetup.stateD);
    Set<String> act1 = Set.of(
        ConstraintTestSetup.actA,
        ConstraintTestSetup.actB,
        ConstraintTestSetup.actD);
    Set<String> act2 = Set.of(ConstraintTestSetup.actC, ConstraintTestSetup.actD);

    var constraint1 = Constraint.create(null, act1, state1, () -> new Windows(ConstraintTestSetup.a), null);
    var constraint2 = Constraint.create(null, act2, state2, () -> new Windows(ConstraintTestSetup.a), null);

    var combined = Constraint.combine(constraint1, constraint2, Windows::subtractAll, null);
    Set<String> actIDs = combined.getActivityIds();
    var stateIDs = combined.getStateIds();

    ConstraintTestSetup.checkActivityIDStrings(actIDs);
    assertEquals(actIDs.size(), 4);

    ConstraintTestSetup.checkStateIDStrings(stateIDs);
    assertEquals(actIDs.size(), 4);
  }

}
