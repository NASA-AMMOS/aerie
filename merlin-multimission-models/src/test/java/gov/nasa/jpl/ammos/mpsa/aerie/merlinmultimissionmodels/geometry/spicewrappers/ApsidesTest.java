package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Body.EARTH;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Body.SUN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;


//TODO: Should become an integration tests and kernels should be stored in Artifcatory.
//      Will require Jenkins configuration.
@Ignore
public class ApsidesTest {

    @BeforeClass
    public static void loadSpiceAndKernels() {
        SpiceLoader.loadSpice();

        String kernelsResourcePath = "/gov/nasa/jpl/ammos/mpsa/aerie/merlinmultimissionmodels/geometry/kernels/";
        String kernelsFilepath = "src/test/resources" + kernelsResourcePath;

        URL lsk = ApsidesTest.class.getResource(kernelsResourcePath + "naif0012.tls");
        URL spk = ApsidesTest.class.getResource(kernelsResourcePath + "de430.bsp");

        String lskPath = null;
        String spkPath = null;

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

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        try {
            CSPICE.furnsh(lskPath);
            CSPICE.furnsh(spkPath);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testApsides() {
        // Perihelion Data: http://www.astropixels.com/ephemeris/perap2001.html
        List<Time> correctPerihelions = List.of(Time.fromTimezoneString("2001-004T08:52:00.0", "UTC"),
                Time.fromTimezoneString("2002-002T14:09:00.0", "UTC"),
                Time.fromTimezoneString("2003-004T05:02:00.0", "UTC"));

        Time start = Time.fromTimezoneString("2001-001T00:00:00.0", "UTC");
        Time end = Time.fromTimezoneString("2004-001T00:00:00.0", "UTC");

        List<Time> apsidesTimes;
        try {
            apsidesTimes = Apsides.apsides(SUN, EARTH, Globals.Apsis.PERIAPSIS,
                    Duration.fromMinutes(1), start, end);

            for (int i = 0; i < apsidesTimes.size(); i++) {
                Time expected = correctPerihelions.get(i);
                Time actual = apsidesTimes.get(i);
                Duration difference = expected.absoluteDifference(actual);

                // assert that the difference between each SPICE-predicted perihelion and actual
                // perihelion is less than one minute (our stepSize)
                String message = "Expected '" + expected.toString() + "' - Actual '" + actual.toString()
                        + "' should be less than 1 minute.";
                assertTrue(message, difference.lessThan(Duration.fromMinutes(1)));
            }

        } catch (SpiceErrorException e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testApsidesFilter() {
        List<Time> aphelionTimes = List.of(Time.fromTimezoneString("2001-185T13:37:00.0", "UTC"),
                Time.fromTimezoneString("2002-187T03:47:00.0", "UTC"),
                Time.fromTimezoneString("2003-185T05:40:00.0", "UTC"));

        // filter distance arbitrarily selected from dry run of what the distances are at the given times
        // it is expected that the 2002 and 2003 aphelions will pass the filter while the 2001 one will not
        double filter = 1.5209e8;

        try {
            List<Time> actualFilteredTimes = Apsides.apsidesFilter(Globals.Apsis.APOAPSIS, filter, aphelionTimes, SUN, EARTH);

            List<Time> expectedFilteredTimes = List.of(Time.fromTimezoneString("2002-187T03:47:00.0", "UTC"),
                    Time.fromTimezoneString("2003-185T05:40:00.0", "UTC"));

            // test that the sizes of the actual and expected lists match
            assertEquals(
                    "Number of expected and actual aphelion times do not match: " + "expected = "
                            + expectedFilteredTimes.size() + " | actual = " + actualFilteredTimes.size(),
                    expectedFilteredTimes.size(), actualFilteredTimes.size());

            // test that the first actual filtered time matches the expected
            assertEquals(
                    "Expected '" + expectedFilteredTimes.get(0) + "' as first filtered time. Got '"
                            + actualFilteredTimes.get(0) + "' instead.",
                    expectedFilteredTimes.get(0), actualFilteredTimes.get(0));

            // test that the second actual filtered time matches the expected
            assertEquals(
                    "Expected '" + expectedFilteredTimes.get(1) + "' as second filtered time. Got '"
                            + actualFilteredTimes.get(1) + "' instead.",
                    expectedFilteredTimes.get(1), actualFilteredTimes.get(1));

        } catch (SpiceErrorException e) {
            e.printStackTrace();
            fail();
        }
    }
}