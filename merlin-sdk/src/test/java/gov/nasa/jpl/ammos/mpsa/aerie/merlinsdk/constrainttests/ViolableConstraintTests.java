package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constrainttests;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ViolableConstraintTests {

  @Test
  public void ViolableConstraintTest() {
    ConstraintTestSetup.checkStateIDStrings(ConstraintTestSetup.violableConstraint.getStateIds());
    ConstraintTestSetup.checkActivityIDStrings(ConstraintTestSetup.violableConstraint.getActivityIds());

    var windows = ConstraintTestSetup.violableConstraint.getWindows();
    Windows expected = new Windows();
    expected.add(0, 3, Duration.SECONDS);
    assertEquals(windows, expected);

    assertEquals(ConstraintTestSetup.violableConstraint.id, ConstraintTestSetup.constraintID);
    assertEquals(ConstraintTestSetup.violableConstraint.name, ConstraintTestSetup.name);
    assertEquals(ConstraintTestSetup.violableConstraint.message, ConstraintTestSetup.message);
    assertEquals(ConstraintTestSetup.violableConstraint.category, ConstraintTestSetup.category);
  }
}
