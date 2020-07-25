package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constrainttests;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.Set;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window.window;
import static org.junit.Assert.assertTrue;

public class ConstraintTestSetup {

  public static Window a = window(0, Duration.SECONDS, 3, Duration.SECONDS);
  public static Window b = window(5, Duration.SECONDS, 8, Duration.SECONDS);
  public static Window c = window(11, Duration.SECONDS, 15, Duration.SECONDS);
  public static Window d = window(20, Duration.SECONDS, 25, Duration.SECONDS);
  public static Window e = window(27, Duration.SECONDS, 29, Duration.SECONDS);

  public static Window alpha = window(6, Duration.SECONDS, 10, Duration.SECONDS);
  public static Window beta = window(11, Duration.SECONDS, 15, Duration.SECONDS);
  public static Window gamma = window(17, Duration.SECONDS, 18, Duration.SECONDS);
  public static Window delta = window(21, Duration.SECONDS, 27, Duration.SECONDS);

  public static String actA = "actA";
  public static String actB = "actB";
  public static String actC = "actC";
  public static String actD = "actD";

  public static String stateA = "stateA";
  public static String stateB = "stateB";
  public static String stateC = "stateC";
  public static String stateD = "stateD";

  public static Set<String> activityIDs = Set.of(actA, actB, actC, actD);
  public static Set<String> stateIDs = Set.of(stateA, stateB, stateC, stateD);

  //Windows: [0,3], [5,8], [11,15], [20,25], [27,29]
  public static Constraint c1 = Constraint.create(() -> new Windows(a, b, c, d, e));

  //Windows: [6,10], [11,15], [17,18], [21,27]
  public static Constraint c2 = Constraint.create(() -> new Windows(alpha, beta, gamma, delta));

  static Constraint constraint = Constraint.create(activityIDs, stateIDs, () -> new Windows(a));

  public static String constraintID = "123";
  public static String name = "A Test Constraint";
  public static String message = "Test Constraint Violated";
  public static String category = "Severe";

  public static ViolableConstraint violableConstraint =
      new ViolableConstraint(constraint)
          .withId(constraintID)
          .withName(name)
          .withMessage(message)
          .withCategory(category);

  public static void checkStateIDStrings(Set<String> stateIDs) {
    assertTrue(stateIDs.contains(ConstraintTestSetup.stateA));
    assertTrue(stateIDs.contains(ConstraintTestSetup.stateB));
    assertTrue(stateIDs.contains(ConstraintTestSetup.stateC));
    assertTrue(stateIDs.contains(ConstraintTestSetup.stateD));
  }

  public static void checkActivityIDStrings(Set<String> actIDs) {
    assertTrue(actIDs.contains(ConstraintTestSetup.actA));
    assertTrue(actIDs.contains(ConstraintTestSetup.actB));
    assertTrue(actIDs.contains(ConstraintTestSetup.actC));
    assertTrue(actIDs.contains(ConstraintTestSetup.actD));
  }

}
