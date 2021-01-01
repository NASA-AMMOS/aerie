package gov.nasa.jpl.ammos.mpsa.aerie.spice;

import org.junit.BeforeClass;
import org.junit.Test;
import spice.basic.CSPICE;

import static org.junit.Assert.assertEquals;

public class SpiceTests {

    @BeforeClass
    public static void loadSpice() {
        SpiceLoader.loadSpice();
    }

    @Test
    public void getSpeedOfLight() {
        assertEquals(299792, CSPICE.clight(), 1);
    }

}
