package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import static org.junit.Assert.assertEquals;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.junit.Test;

public class ParameterBuilderTests {

  @Test
  public void testParameterBuilderCanSetName() {
    String name = "size";
    ParameterBuilder builder = new ParameterBuilder().withName(name);
    Parameter parameter = builder.getParameter();
    assertEquals(parameter.getName(), name);
  }

  @Test
  public void testParameterBuilderCanSetType() {
    Object type = Integer.class;
    ParameterBuilder builder = new ParameterBuilder().ofType(type);
    Parameter parameter = builder.getParameter();
    assertEquals(parameter.getType(), type);
  }

  @Test
  public void testParameterBuilderCanSetValue() {
    Object value = 0;
    ParameterBuilder builder = new ParameterBuilder().withValue(value);
    Parameter parameter = builder.getParameter();
    assertEquals(parameter.getValue(), value);
  }

  @Test
  public void testParameterBuilderCanSetAsReadOnly() {
    boolean readOnly = true;
    ParameterBuilder builder = new ParameterBuilder().asReadOnly(readOnly);
    Parameter parameter = builder.getParameter();
    assertEquals(parameter.isReadOnly(), readOnly);
  }
}