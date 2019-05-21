package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice;

import spice.basic.CSPICE;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

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