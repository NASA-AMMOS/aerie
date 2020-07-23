package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constrainttests;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConstraintViolationTest {

  @Test
  public void ConstraintViolationTest() {
    var violation = new ConstraintViolation(
        new Windows(ConstraintTestSetup.a, ConstraintTestSetup.b),
        ConstraintTestSetup.violableConstraint);

    assertEquals(violation.id, ConstraintTestSetup.constraintID);
    assertEquals(violation.name, ConstraintTestSetup.name);
    assertEquals(violation.message, ConstraintTestSetup.message);
    assertEquals(violation.category, ConstraintTestSetup.category);

    ConstraintTestSetup.checkActivityIDStrings(violation.associatedActivityIds);
    ConstraintTestSetup.checkStateIDStrings(violation.associatedStateIds);

    var windows = violation.violationWindows;
    Windows expected = new Windows();
    expected.add(0, TimeUnit.SECONDS, 3, TimeUnit.SECONDS);
    expected.add(5, TimeUnit.SECONDS, 8, TimeUnit.SECONDS);
    assertEquals(windows, expected);
  }

}
