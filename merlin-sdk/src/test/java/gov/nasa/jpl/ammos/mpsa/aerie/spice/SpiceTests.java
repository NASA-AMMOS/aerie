package gov.nasa.jpl.ammos.mpsa.aerie.spice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import spice.basic.CSPICE;

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
