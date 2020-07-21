package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

/*
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Apsis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Body;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers.Apsides;
*/
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

public class Geometry {

    public static void loadSpiceAndKernels() {
        SpiceLoader.loadSpice();

        String kernelsResourcePath = "/gov/nasa/jpl/ammos/mpsa/aerie/sampleadaptation/kernels/";
        String kernelsFilepath = "src/main/resources" + kernelsResourcePath;

        // could add jup310.bsp to be able to use JUPITER rather than JUPITER_BARYCENTER, but it's 1.1GB in size
        URL lsk = Geometry.class.getResource(kernelsResourcePath + "naif0012.tls");
        URL spk = Geometry.class.getResource(kernelsResourcePath + "de430.bsp");
        URL spk2 = Geometry.class.getResource(kernelsResourcePath + "juno_rec_orbit.bsp");
        URL pck = Geometry.class.getResource(kernelsResourcePath + "pck00010.tpc");

        String lskPath = null;
        String spkPath = null;
        String spk2Path = null;
        String pckPath = null;

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

            if (spk2 == null) {
                System.out.println("Downloading 'juno_rec_orbit.bsp'...");
                URL source = new URL("https://naif.jpl.nasa.gov/pub/naif/JUNO/kernels/spk/juno_rec_orbit.bsp");
                File destination = new File(kernelsFilepath + "juno_rec_orbit.bsp");
                FileUtils.copyURLToFile(source, destination);
                spk2Path = destination.getAbsolutePath();
            } else {
                spk2Path = spk2.getPath();
            }

            if (pck == null) {
                System.out.println("Downloading 'pck00010.tpc'...");
                URL source = new URL("https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck/pck00010.tpc");
                File destination = new File(kernelsFilepath + "pck00010.tpc");
                FileUtils.copyURLToFile(source, destination);
                pckPath = destination.getAbsolutePath();
            } else {
                pckPath = pck.getPath();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            CSPICE.furnsh(lskPath);
            CSPICE.furnsh(spkPath);
            CSPICE.furnsh(spk2Path);
            CSPICE.furnsh(pckPath);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
    }

    /*
    public static List<Time> getPeriapsides(Config config) {
        Body target = Body.SUN;
        Body observer = Body.JUNO;
        Apsis apsisType = Apsis.PERIAPSIS;
        Duration stepSize = Duration.fromMinutes(1);
        Time start = config.missionStartTime;
        Time end = config.missionEndTime;
        try {
            return Apsides.apsides(target, observer, apsisType, stepSize, start, end);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static List<Time> getApoapsides(Config config) {
        Body target = Body.SUN;
        Body observer = Body.JUNO;
        Apsis apsisType = Apsis.APOAPSIS;
        Duration stepSize = Duration.fromMinutes(1);
        Time start = config.missionStartTime;
        Time end = config.missionEndTime;
        try {
            return Apsides.apsides(target, observer, apsisType, stepSize, start, end);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
            return List.of();
        }
    }
    */
}
