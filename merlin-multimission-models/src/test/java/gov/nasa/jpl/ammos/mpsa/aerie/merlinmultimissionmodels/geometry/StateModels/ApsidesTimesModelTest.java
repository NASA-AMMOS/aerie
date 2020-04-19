package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Apsis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Body;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers.ApsidesTest;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

@Ignore
public class ApsidesTimesModelTest {
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
    public void testApsidesTimesModel() {
        final var simulationEngine = new SimulationEngine();

        // Perihelion Data: http://www.astropixels.com/ephemeris/perap2001.html
        // NOTE: only the first of these is expected in the first `get()`; both are
        // expected after the second `get()`
        final var expectedAphelionTimes = List.of(
            Time.fromTimezoneString("2002-187T03:47:00.0", "UTC"),
            Time.fromTimezoneString("2003-185T05:40:00.0", "UTC"));

        final var earthSunApsidesModel = new ApsidesTimesModel();
        earthSunApsidesModel.initialize(simulationEngine.getCurrentTime());
        earthSunApsidesModel.setStart(Time.fromTimezoneString("2001-001T00:00:00.0", "UTC"));
        earthSunApsidesModel.setFilter(1.5209e8);
        earthSunApsidesModel.setTarget(Body.SUN);
        earthSunApsidesModel.setObserver(Body.EARTH);
        earthSunApsidesModel.setApsisType(Apsis.APOAPSIS);

        // test that the difference between each SPICE-predicted perihelion
        // and actual perihelion is less than one minute (our stepSize)

        simulationEngine.scheduleJobAfter(Duration.ZERO, SimulationEffects.withEffects(() -> {
            earthSunApsidesModel.setEnd(Time.fromTimezoneString("2003-001T00:00:00.0", "UTC"));

            final List<Time> aphelionTimes = earthSunApsidesModel.get();
            assertEquals("1 aphelion time expected; '" + aphelionTimes.size() + "' received.", 1, aphelionTimes.size());

            for (int i = 0; i < aphelionTimes.size(); i++) {
                final Time expected = expectedAphelionTimes.get(i);
                final Time actual = aphelionTimes.get(i);
                final var difference = expected.absoluteDifference(actual);

                // assert that the difference between each SPICE-predicted perihelion and actual
                // perihelion is less than one minute (our stepSize)
                assertTrue(
                    "Expected '" + expected.toString() + "' - Actual '" + actual.toString()
                        + "' should be less than 1 minute.",
                    difference
                        .lessThan(gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration.fromMinutes(1)));
            }
        }));

        simulationEngine.scheduleJobAfter(Duration.ZERO, SimulationEffects.withEffects(() -> {
            earthSunApsidesModel.setEnd(Time.fromTimezoneString("2004-001T00:00:00.0", "UTC"));

            final List<Time> aphelionTimes = earthSunApsidesModel.get();
            assertEquals("2 aphelion times expected; '" + aphelionTimes.size() + "' received.", 2, aphelionTimes.size());

            for (int i = 0; i < aphelionTimes.size(); i++) {
                final Time expected = expectedAphelionTimes.get(i);
                final Time actual = aphelionTimes.get(i);
                final var difference = expected.absoluteDifference(actual);

                assertTrue(
                    "Expected '" + expected.toString() + "' - Actual '" + actual.toString()
                        + "' should be less than 1 minute.",
                    difference
                        .lessThan(gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration.fromMinutes(1)));
            }
        }));

        simulationEngine.runToCompletion();
    }
}
