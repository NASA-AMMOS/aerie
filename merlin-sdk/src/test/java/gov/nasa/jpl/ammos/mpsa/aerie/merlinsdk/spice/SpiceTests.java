package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

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