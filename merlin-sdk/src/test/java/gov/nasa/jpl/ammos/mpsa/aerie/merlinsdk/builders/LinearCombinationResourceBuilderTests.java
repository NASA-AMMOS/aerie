package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.LinearCombinationResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import org.junit.Test;

public class LinearCombinationResourceBuilderTests {

    @Test
    public void testLinearCombinationResourceCreationFromDoubleResources() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").ofType(Double.class).withInitialValue(1.2)
                .getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").ofType(Double.class).withInitialValue(2.2)
                .getResource();
        LinearCombinationResource totalPowerDraw = new LinearCombinationResource();
        HashMap<Resource, Integer> terms = new HashMap<Resource, Integer>();
        terms.put(powerDraw1, 1);
        terms.put(powerDraw2, 1);
        totalPowerDraw.setTerms(terms);
        assertEquals(3.4, (double) totalPowerDraw.getCurrentValue(), 1e-15);
    }

    @Test
    public void testLinearCombinationResourceCreationFromIntegerResources() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").ofType(Integer.class).withInitialValue(1)
                .getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").ofType(Integer.class).withInitialValue(2)
                .getResource();
        LinearCombinationResource totalPowerDraw = new LinearCombinationResource();
        HashMap<Resource, Integer> terms = new HashMap<Resource, Integer>();
        terms.put(powerDraw1, 1);
        terms.put(powerDraw2, 1);
        totalPowerDraw.setTerms(terms);
        assertEquals(3.0, (double) totalPowerDraw.getCurrentValue(), 1e-15);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testLinearCombinationResourceCreationFromStringResources() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").ofType(String.class).withInitialValue("A")
                .getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").ofType(String.class).withInitialValue("B")
                .getResource();
        LinearCombinationResource totalPowerDraw = new LinearCombinationResource();
        HashMap<Resource, Integer> terms = new HashMap<Resource, Integer>();
        terms.put(powerDraw1, 1);
        terms.put(powerDraw2, 1);
        totalPowerDraw.setTerms(terms);
    }

    @Test
    public void testLinearCombinationResourcePropertyChange() {
        Resource powerDraw1 = new ResourceBuilder().withName("Power_Draw_1").ofType(Double.class).withInitialValue(1.2)
                .getResource();
        Resource powerDraw2 = new ResourceBuilder().withName("Power_Draw_2").ofType(Double.class).withInitialValue(2.2)
                .getResource();
        LinearCombinationResource totalPowerDraw = new LinearCombinationResource();
        HashMap<Resource, Integer> terms = new HashMap<Resource, Integer>();
        terms.put(powerDraw1, 1);
        terms.put(powerDraw2, 1);
        totalPowerDraw.setTerms(terms);
        assertEquals(3.4, (double) totalPowerDraw.getCurrentValue(), 1e-15);
        powerDraw1.setValue(1.1);
        assertEquals(3.3, (double) totalPowerDraw.getCurrentValue(), 1e-15);
    }

}
