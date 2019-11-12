package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.ABCORR.LTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

/**
 * The Blackbird (BB) MultiMission (MM) Geometry Model was used as a reference for this software
 * The BB MM code can be captured here: https://github.jpl.nasa.gov/Blackbird/MultiMissionModels/tree/master/blackbird-geometrymodel
 * The CSPICE source can be found by going to the NAIF website, and locally downloading the files: https://naif.jpl.nasa.gov/pub/naif/misc/JNISpice/
 */

//TODO: Have SPICE I/O table have the actual names of the arguments used in methods (e.g. change relate to relationship)
public class Apsides {
    /**
     * Return the time window over which a specified constraint on
     *    observer-target distance is met.
     *
     * Brief_I/O
     *
     *    Variable         I/O  Description
     *    ---------------  ---  ------------------------------------------------
     *    SPICE_GF_CNVTOL   P   Convergence tolerance
     *    target            I   Name of the target body.  EUROPA
     *    abcorr            I   Aberration correction flag.  NONE
     *    obsrvr            I   Name of the observing body.  CLIPPER
     *    relate            I   Relational operator.  LOCMAX
     *    refval            I   Reference value.  NONE
     *    adjust            I   Adjustment value for absolute extrema searches.  NONE
     *    step              I   Step size used for locating extrema and roots.  inputted param
     *    nintvls           I   Workspace window interval count. calculated from params
     *    cnfine           I-O  SPICE window to which the search is confined.
     *    result            O   SPICE window containing results.
     */
    public static List<Time> apsides(Globals.Body target, Globals.Body observer, Globals.Apsis apsis,
                                     Duration stepSize, Time start, Time end) throws SpiceErrorException {
        // SPICE-suggested number of intervals
        int intervalCount = 2 + (int) Math.ceil((end.subtract(start)).totalSeconds() / stepSize.totalSeconds());
        // need to create a SPICE window with ephemeris time
        double[] confinementWindow = new double[] { start.toET(), end.toET() };

        String relationship;

        switch (apsis) {
            case APOAPSIS:
                relationship = "LOCMAX";
                break;
            case PERIAPSIS:
                relationship = "LOCMIN";
                break;
            default:
                throw new Error("unexpected " + apsis.getClass().getSimpleName() + " with value " + String.valueOf(apsis));
        }

        double[] apsidesTimes = CSPICE.gfdist(target.toString(), "NONE", observer.toString(), relationship, 0.0, 0.0,
                stepSize.totalSeconds(), intervalCount, confinementWindow);

        return Arrays
                .stream(apsidesTimes)
                .mapToObj(Time::fromET)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     *  Return the state (position and velocity) of a target body
     *    relative to an observing body, optionally corrected for light
     *    time (planetary aberration) and stellar aberration.
     *
     *    Variable  I/O  Description
     *    --------  ---  --------------------------------------------------
     *    targ       I   Target body name.
     *    et         I   Observer epoch.  QUESTION: I'm assuming the gfdist method returns ET right??
     *    ref        I   Reference frame of output state vector.
     *    abcorr     I   Aberration correction flag.
     *    obs        I   Observing body name.
     *    starg      O   State of target.
     *    lt         O   One way light time between observer and target.
     */
    //TODO: Any situation when we need a different ABCORR?
    public static List<Time> apsidesFilter(Globals.Apsis apsisType, double filter, List<Time> apsidesTimes, Globals.Body target, Globals.Body observer)
            throws SpiceErrorException {
        List<Time> filteredTimes = new ArrayList<>();

        for (Time apsisTime: apsidesTimes) {

            Vector3D[] positionAndVelocity = new Vector3D[2];
            double[] state = new double[6];

            CSPICE.spkezr(target.toString(), apsisTime.toET(), "J2000", LTS.type, observer.toString(), state,
                    new double[1]);

            positionAndVelocity[0] = new Vector3D(state[0], state[1], state[2]);

            boolean passesFilter;
            switch (apsisType) {
            case APOAPSIS:
                passesFilter = positionAndVelocity[0].getNorm() >= filter;
                break;
            case PERIAPSIS:
                passesFilter = positionAndVelocity[0].getNorm() <= filter;
                break;
            default:
                throw new Error("unexpected " + apsisType.getClass().getSimpleName() + " with value " + String.valueOf(apsisType));
            }

            if (passesFilter) {
                filteredTimes.add(apsisTime);
            }
        }

        return filteredTimes;
    }
}
