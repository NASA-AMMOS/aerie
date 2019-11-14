package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.classes.Eclipse;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


//TODO: Should write tests that stress eclipse-searching logic better
public class EclipsesTest {

    @BeforeClass
    public static void loadSpiceAndKernels() {
        SpiceLoader.loadSpice();

        String kernelsResourcePath = "/gov/nasa/jpl/ammos/mpsa/aerie/merlinmultimissionmodels/geometry/kernels/";
        String kernelsFilepath = "src/test/resources" + kernelsResourcePath;

        URL lsk = OccultationsTest.class.getResource(kernelsResourcePath + "naif0012.tls");
        URL spk = OccultationsTest.class.getResource(kernelsResourcePath + "de430.bsp");
        URL fk = OccultationsTest.class.getResource(kernelsResourcePath + "moon_080317.tf");
        URL pck = OccultationsTest.class.getResource(kernelsResourcePath + "moon_pa_de421_1900-2050.bpc");
        URL pck2 = OccultationsTest.class.getResource(kernelsResourcePath + "pck00010.tpc");

        String lskPath = null;
        String spkPath = null;
        String fkPath = null;
        String pckPath = null;
        String pck2Path = null;

        try {
            // Download the kernels if they aren't present within the maven project
            if (lsk == null) {
                System.out.println("Downloading 'naif0012.tls'...");
                URL source = new URL("https://naif.jpl.nasa.gov/pub/naif/generic_kernels/lsk/naif0012.tls");
                File destination = new File(kernelsFilepath + "naif0012.tls");
                FileUtils.copyURLToFile(source, destination);
                lskPath = destination.getAbsolutePath();
            } else {
                lskPath = lsk.getPath();
            }

            if (spk == null) {
                System.out.println("Downloading 'de430.bsp'...");
                URL source = new URL("https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de430.bsp");
                File destination = new File(kernelsFilepath + "de430.bsp");
                FileUtils.copyURLToFile(source, destination);
                spkPath = destination.getAbsolutePath();
            } else {
                spkPath = spk.getPath();
            }

            if (fk == null) {
                System.out.println("Downloading 'moon_080317.tf'...");
                URL source = new URL("https://naif.jpl.nasa.gov/pub/naif/generic_kernels/fk/satellites/moon_080317.tf");
                File destination = new File(kernelsFilepath + "moon_080317.tf");
                FileUtils.copyURLToFile(source, destination);
                fkPath = destination.getAbsolutePath();
            } else {
                fkPath = fk.getPath();
            }

            if (pck == null) {
                System.out.println("Downloading 'moon_pa_de421_1900-2050.bpc'...");
                URL source = new URL("https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/moon_pa_de421_1900-2050.bpc");
                File destination = new File(kernelsFilepath + "moon_pa_de421_1900-2050.bpc");
                FileUtils.copyURLToFile(source, destination);
                pckPath = destination.getAbsolutePath();
            } else {
                pckPath = pck.getPath();
            }

            if (pck2 == null) {
                System.out.println("Downloading 'pck00010.tpc'...");
                URL source = new URL("https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00010.tpc");
                File destination = new File(kernelsFilepath + "pck00010.tpc");
                FileUtils.copyURLToFile(source, destination);
                pck2Path = destination.getAbsolutePath();
            } else {
                pck2Path = pck2.getPath();
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        try {
            CSPICE.furnsh(lskPath);
            CSPICE.furnsh(spkPath);
            CSPICE.furnsh(fkPath);
            CSPICE.furnsh(pckPath);
            CSPICE.furnsh(pck2Path);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testEclipses() {

        Time start = Time.fromTimezoneString("2001-335T00:00:00.0", "UTC");
        Time end = Time.fromTimezoneString("2002-001T00:00:00.0", "UTC");

        Body frontBody = Body.MOON;
        ReferenceFrame frontFrame = ReferenceFrame.IAU_MOON;
        Body backBody = Body.SUN;
        ReferenceFrame backFrame = ReferenceFrame.IAU_SUN;
        Body observer = Body.EARTH;
        Duration step = Duration.fromMinutes(3);

        try {
            List<Eclipse> eclipseTimes = Eclipses.allEclipses(frontBody, frontFrame, backBody, backFrame, observer,
                    step, start, end);

            Time expectedStart = Time.fromTimezoneString("2001-348T20:10:14.195952", "UTC");
            Time expectedEnd = Time.fromTimezoneString("2001-348T21:35:50.317994", "UTC");
            
            Time actualStart = eclipseTimes.get(0).getStart();
            Time actualEnd = eclipseTimes.get(0).getEnd();

            // assert that the difference between each SPICE-predicted occultation and actual
            // occultation is less than three minutes (our stepSize)

            Duration startDifference = expectedStart.absoluteDifference(actualStart);
            String message = "Expected start '" + expectedStart.toString() + "' - Actual start '" + actualStart.toString()
                    + "' should be less than 3 minutes.";
            assertTrue(message, startDifference.lessThan(Duration.fromMinutes(3)));

            Duration endDifference = expectedEnd.absoluteDifference(actualEnd);
            message = "Expected end '" + expectedEnd.toString() + "' - Actual end '" + actualEnd.toString()
                    + "' should be less than 3 minutes.";
            assertTrue(message, endDifference.lessThan(Duration.fromMinutes(3)));

        } catch (SpiceErrorException e) {
            e.printStackTrace();
            fail();
        }
    }

}