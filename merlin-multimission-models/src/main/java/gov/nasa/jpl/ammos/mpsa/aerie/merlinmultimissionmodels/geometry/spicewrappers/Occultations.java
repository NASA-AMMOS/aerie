package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.blackbird.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.blackbird.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.*;
import org.apache.commons.lang3.tuple.Pair;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.util.ArrayList;
import java.util.List;

public class Occultations {


    /**
     *   Determine time intervals when an observer sees one target occulted
     *   by, or in transit across, another.
     *
     *   The surfaces of the target bodies may be represented by triaxial
     *   ellipsoids or by topographic data provided by DSK files.
     *
     *    VARIABLE        I/O  DESCRIPTION
     *    --------------- ---  -------------------------------------------------
     *    SPICE_GF_CNVTOL  P   Convergence tolerance.
     *    occtyp           I   Type of occultation.
     *    front            I   Name of body occulting the other.
     *    fshape           I   Type of shape model used for front body.
     *    fframe           I   Body-fixed, body-centered frame for front body.
     *    back             I   Name of body occulted by the other.
     *    bshape           I   Type of shape model used for back body.
     *    bframe           I   Body-fixed, body-centered frame for back body.
     *    abcorr           I   Aberration correction flag.
     *    obsrvr           I   Name of the observing body.
     *    step             I   Step size in seconds for finding occultation
     *                         events.
     *    cnfine          I-O  SPICE window to which the search is restricted.
     *    result           O   SPICE window containing results.
     */

    public static List<Pair<Time, Time>> occultations(OccultationType occType, Body frontBody, Shape frontShape,
            ReferenceFrame frontFrame, Body backBody, Shape backShape, ReferenceFrame backFrame, ABCORR abcorr,
            Body observer, Duration step, Time start, Time end) throws SpiceErrorException {

        // SPICE-suggested number of intervals
        int intervalCount = 2 + (int) Math.ceil((end.subtract(start)).totalSeconds() / step.totalSeconds());
        // need to create a SPICE window with ephemeris time
        double[] confinementWindow = new double[] { start.toET(), end.toET() };

        double[] occultationResults = CSPICE.gfoclt(occType.toString(), frontBody.toString(), frontShape.toString(),
                frontFrame.toString(), backBody.toString(), backShape.toString(), backFrame.toString(), abcorr.type,
                observer.toString(), step.totalSeconds(), intervalCount, confinementWindow);
        
        List<Pair<Time, Time>> occultationWindows = new ArrayList<>();
        for (int i = 0; i < occultationResults.length; i += 2) {
            Pair<Time, Time> window = Pair.of(Time.fromET(occultationResults[i]), Time.fromET(occultationResults[i + 1]));
            occultationWindows.add(window);
        }

        return occultationWindows;
    }


    public static List<Pair<Time, Time>> occultations(Body frontBody, ReferenceFrame frontFrame, Body backBody, ReferenceFrame backFrame,
                                                      Body observer, Duration step, Time start, Time end) throws SpiceErrorException {
        return occultations(OccultationType.ANY, frontBody, Shape.ELLIPSOID, frontFrame, backBody, Shape.ELLIPSOID,
                backFrame, ABCORR.LT, observer, step, start, end);
    }

}

