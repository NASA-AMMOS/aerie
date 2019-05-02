package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashSet;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.LinearCombinationResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

public class ResourceBuilderTests {

  @Test
  public void testResourceBuilderCanSetName() {
    String name = "peel";
    ResourceBuilder builder = new ResourceBuilder().withName(name);
    Resource resource = builder.getResource();
    assertSame(resource.getName(), name);
  }

  @Test
  public void testResourceBuilderCanSetType() {
    ResourceBuilder builder = new ResourceBuilder().ofType(Integer.class);
    Resource resource = builder.getResource();
    assertSame(resource.getType(), Integer.class);
  }

  @Test
  public void testResourceBuilderCanSetInitialValue() {
    Object val = 3;
    ResourceBuilder builder = new ResourceBuilder().withInitialValue(val);
    Resource resource = builder.getResource();
    assertSame(resource.getCurrentValue(), val);
  }

  @Test
  public void testResourceBuilderCanSetSubsystem() {
    String subsystem= "peel";
    ResourceBuilder builder = new ResourceBuilder().forSubsystem(subsystem);
    Resource resource = builder.getResource();
    assertSame(resource.getSubsystem(), subsystem);
  }

  @Test
  public void testResourceBuilderCanSetUnits() {
    String units = "sections";
    ResourceBuilder builder = new ResourceBuilder().withUnits(units);
    Resource resource = builder.getResource();
    assertSame(resource.getUnits(), units);
  }

  @Test
  public void testResourceBuilderCanSetInterpolation() {
    String interpolation = "linear";
    ResourceBuilder builder = new ResourceBuilder().withInterpolation(interpolation);
    Resource resource = builder.getResource();
    assertSame(resource.getInterpolation(), interpolation);
  }

  @Test
  public void testResourceBuilderCanSetAllowedValues() {
    HashSet<Integer> allowedValues = new HashSet<>();
    ResourceBuilder builder = new ResourceBuilder().withAllowedValues(allowedValues);
    Resource resource = builder.getResource();
    assertSame(resource.getAllowedValues(), allowedValues);
  }

  // TODO: Move to Resource tests when they exist
  @Test
  public void testResourceBuilderCanSetAnAllowedValue() {
    Integer value = 3;
    HashSet<Integer> allowedValues = new HashSet<>();
    allowedValues.add(value);
    ResourceBuilder builder = new ResourceBuilder().withAllowedValues(allowedValues);
    Resource resource = builder.getResource();
    resource.setValue(value);
    assertSame(resource.getCurrentValue(), value);
  }

  // TODO: Move to Resource tests when they exist
  @Test
  public void testResourceBuilderCannotSetAnUnspecififedValue() {
    Integer value = 3;
    HashSet<Integer> allowedValues = new HashSet<>(); // No allowed values
    ResourceBuilder builder = new ResourceBuilder().withAllowedValues(allowedValues);
    Resource resource = builder.getResource();

    // Expect throw
    boolean caughtError = false;
    try {
      resource.setValue(value);
    } catch (RuntimeException e) {
      caughtError = true;
    }

    assertSame(resource.getCurrentValue(), null);
    assertSame(caughtError, true);
  }

  @Test
  public void testResourceBuilderCanSetMinimum() {
    Integer min = 7;
    ResourceBuilder builder = new ResourceBuilder().withMin(min);
    Resource resource = builder.getResource();
    assertSame(resource.getMinimum(), min);
  }

  @Test
  public void testResourceBuilderCanSetMaximum() {
    Integer max = 7;
    ResourceBuilder builder = new ResourceBuilder().withMax(max);
    Resource resource = builder.getResource();
    assertSame(resource.getMaximum(), max);
  }

  @Test
  public void testResourceBuilderCanSetFrozen() {
    boolean isFrozen = false;
    ResourceBuilder builder = new ResourceBuilder().isFrozen(isFrozen);
    Resource resource = builder.getResource();
    assertSame(resource.isFrozen(), isFrozen);
  }

  @Test
  public void testResourceBuilderLinearCombinationFromDoubleResources() {
    Resource powerDraw1 = new ResourceBuilder()
                .withName("Power_Draw_1")
                .withInitialValue(1.2)
                .getResource();
    Resource powerDraw2 = new ResourceBuilder()
                .withName("Power_Draw_2")
                .withInitialValue(2.2)
                .getResource();
    Resource totalPowerDraw = new ResourceBuilder(LinearCombinationResource.class)
                .withName("Total_Power_Draw")
                .withTerm(powerDraw1, 1)
                .withTerm(powerDraw2, 1)
                .getResource();
    assertEquals(3.4, (double) totalPowerDraw.getCurrentValue(), 1e-15);
  }

  @Test
  public void testResourceBuilderLinearCombinationFromIntegerResources() {
    Resource powerDraw1 = new ResourceBuilder()
                .withName("Power_Draw_1")
                .withInitialValue(1)
                .getResource();
    Resource powerDraw2 = new ResourceBuilder()
                .withName("Power_Draw_2")
                .withInitialValue(2)
                .getResource();
    Resource totalPowerDraw = new ResourceBuilder(LinearCombinationResource.class)
                .withName("Total_Power_Draw")
                .withTerm(powerDraw1, 1)
                .withTerm(powerDraw2, 1)
                .getResource();
    assertEquals(3.0, (double) totalPowerDraw.getCurrentValue(), 1e-15);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testResourceBuilderLinearCombinationFromStringResources() {
    Resource powerDraw1 = new ResourceBuilder()
                .withName("Power_Draw_1")
                .withInitialValue("A")
                .getResource();
    Resource powerDraw2 = new ResourceBuilder()
                .withName("Power_Draw_2")
                .withInitialValue("B")
                .getResource();
    Resource totalPowerDraw = new ResourceBuilder(LinearCombinationResource.class)
                .withName("Total_Power_Draw")
                .withTerm(powerDraw1, 1)
                .withTerm(powerDraw2, 1)
                .getResource();
  }

  @Test
  public void testLinearCombinationResourcePropertyChange() {
    Resource powerDraw1 = new ResourceBuilder()
                .withName("Power_Draw_1")
                .withInitialValue(1.2)
                .getResource();
    Resource powerDraw2 = new ResourceBuilder()
                .withName("Power_Draw_2")
                .withInitialValue(2.2)
                .getResource();
    Resource totalPowerDraw = new ResourceBuilder(LinearCombinationResource.class)
                .withName("Total_Power_Draw")
                .withTerm(powerDraw1, 1)
                .withTerm(powerDraw2, 1)
                .getResource();
    powerDraw1.setValue(0.2);
    assertEquals(2.4, (double) totalPowerDraw.getCurrentValue(), 1e-15);
  }

}
