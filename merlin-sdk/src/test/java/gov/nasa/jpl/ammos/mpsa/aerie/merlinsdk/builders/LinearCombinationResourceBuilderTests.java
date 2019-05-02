package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class LinearCombinationResourceBuilderTests {

    @Test
    public void testLinearCombinationResourceCreationFromDoubleResources() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").withInitialValue(1.2).getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").withInitialValue(2.2).getResource();
        ImmutableMap<Resource, Number> terms = ImmutableMap.of(powerDraw1, 1, powerDraw2, 1);
        ResourceBuilder builder = new LinearCombinationResourceBuilder(terms).withName("Total_Power_Draw");
        Resource totalPowerDraw = builder.getResource();
        assertEquals(3.4, (double) totalPowerDraw.getCurrentValue(), 1e-15);
    }

    @Test
    public void testLinearCombinationResourceCreationFromIntegerResources() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").withInitialValue(1).getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").withInitialValue(2).getResource();
        ImmutableMap<Resource, Number> terms = ImmutableMap.of(powerDraw1, 1, powerDraw2, 1);
        ResourceBuilder builder = new LinearCombinationResourceBuilder(terms).withName("Total_Power_Draw");
        Resource totalPowerDraw = builder.getResource();
        assertEquals(3.0, (double) totalPowerDraw.getCurrentValue(), 1e-15);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLinearCombinationResourceCreationFromStringResources() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").withInitialValue("A").getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").withInitialValue("B").getResource();
        ImmutableMap<Resource, Number> terms = ImmutableMap.of(powerDraw1, 1, powerDraw2, 1);
        ResourceBuilder builder = new LinearCombinationResourceBuilder(terms).withName("Total_Power_Draw");
    }

    @Test
    public void testLinearCombinationResourcePropertyChange() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").withInitialValue(1.2).getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").withInitialValue(2.2).getResource();
        ImmutableMap<Resource, Number> terms = ImmutableMap.of(powerDraw1, 1, powerDraw2, 1);
        ResourceBuilder builder = new LinearCombinationResourceBuilder(terms).withName("Total_Power_Draw");
        Resource totalPowerDraw = builder.getResource();
        assertEquals(3.4, (double) totalPowerDraw.getCurrentValue(), 1e-15);
        powerDraw1.setValue(1.1);
        assertEquals(3.3, (double) totalPowerDraw.getCurrentValue(), 1e-15);
    }

}
