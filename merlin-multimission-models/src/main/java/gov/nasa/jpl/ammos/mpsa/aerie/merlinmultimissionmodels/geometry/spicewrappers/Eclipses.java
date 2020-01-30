package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.classes.Eclipse;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import org.apache.commons.lang3.tuple.Pair;
import spice.basic.SpiceErrorException;

import java.util.ArrayList;
import java.util.List;

//This logic is from the following BB file:
//https://github.jpl.nasa.gov/Blackbird/MultiMissionModels/blob/master/blackbird-geometrymodel/src/main/java/gov/nasa/jpl/geometrymodel/GeometricFunctions.java
//BB took this mostly from Steve Wissler

public class Eclipses {

    public static List<Eclipse> allEclipses(
            Body frontBody, ReferenceFrame frontFrame,
            Body backBody, ReferenceFrame backFrame,
            Body observer,
            Duration step, Time start, Time end) throws SpiceErrorException {

        List<Eclipse> foundEclipses = new ArrayList<>();

        // we need to what type of eclipse each was. although Spice has an "ANY"
        // occultation type, we only get back time results and we need to know what
        // *type* it was as well
        List<OccultationType> occulationTypes = List.of(OccultationType.ANNULAR, OccultationType.PARTIAL,
                OccultationType.FULL);

        for (OccultationType type : occulationTypes) {

            List<Pair<Time, Time>> eclipseWindows = Occultations.occultations(type, frontBody, Shape.ELLIPSOID, frontFrame,
                    backBody, Shape.ELLIPSOID, backFrame, ABCORR.CN, observer, step, start, end);

            for (Pair<Time, Time> eclipseWindow : eclipseWindows) {
                // TODO: replace with correct solar visibility logic
                //       we are currently assuming that any eclipse fully occludes the Sun
                double fracSunVisible;
                switch (type) {
                case ANNULAR:
                case PARTIAL:
                case FULL:
                    fracSunVisible = 0.0;
                    break;
                default:
                    throw new Error(
                            "unexpected " + type.getClass().getSimpleName() + " with value " + String.valueOf(type));
                }
                foundEclipses.add(new Eclipse(eclipseWindow.getLeft(), eclipseWindow.getRight(), type, fracSunVisible));
            }
        }

        return addMissingPartialEclipses(foundEclipses, frontBody, frontFrame, backBody, backFrame, observer);
    }

    // I/O:
    // This takes in a List of Eclipse types and sorts them by their start time (earliest start time is first in the list)
    // If a partial eclipse is not already in the list before a full eclipse or after a full eclipse, this method
    // will search for a partial eclipse with a smaller step size.  If a partial eclipse is found, it is added to the list,
    // modifying the list.
    // The modified list is then returned.

    // Method descipriton:
    // if we have a full eclipse, this means that there must be a partial eclipse
    // immediately before and after the full eclipse entry in the list
    // this partial eclipse must happen within a second of the full eclipse,
    // otherwise it does not belong to the full eclipse we are considering
    // if we do not have partial eclipses bookending this full eclipse within a
    // second, we will decrease the step size and try to find them

    // Background:
    // I am following Blackbird's logic for this
    // blackbird adds a partial eclipse with a duration of 1 second if the list
    // returned after searching for partial eclipsesis empty. I think this is in
    // part b/c they are thinking of
    // displaying this in a UI. I am not doing so b/c 1). I do not want to modify
    // spice results and 2). if we can't find a partial eclipse we should try
    // rerunning this and decreasing the step
    // also we should throw a warning if this list is greater than 1
    public static List<Eclipse> addMissingPartialEclipses(List<Eclipse> eclipses, Body frontBody,
            ReferenceFrame frontFrame, Body backBody, ReferenceFrame backFrame, Body observer)
            throws SpiceErrorException {

        // sort all the eclipses in order by when they start (earliest start time is
        // first)
        eclipses.sort((a, b) -> a.getStart().compareTo(b.getStart()));

        int numEclipses = eclipses.size();

        if (numEclipses == 1) {
            return eclipses;
        }

        for (int i = 0; i < numEclipses; i++) {

            OccultationType eclipseType = eclipses.get(i).getEclipseType();
            Time eclipseStart = eclipses.get(i).getStart();
            Time eclipseEnd = eclipses.get(i).getEnd();
            Duration minDurationBtwnEclipses = new Duration("00:00:01");

            //this is the first index in the list; we know that a partial eclipse entry is missing
            if (i == 0 && eclipseType == OccultationType.FULL) {
                List<Pair<Time, Time>> missedEclipses = Occultations.occultations(OccultationType.PARTIAL, frontBody,
                        Shape.ELLIPSOID, frontFrame, backBody, Shape.ELLIPSOID, backFrame, ABCORR.CN, observer,
                        new Duration("00:00:00.5"), eclipseStart.subtract(new Duration("00:05:00")),
                        eclipseStart.add(new Duration("00:05:00")));
                
                // TODO: compute correct fracSunVisible
                Pair<Time, Time> missedEclipseWindow = missedEclipses.get(0);
                eclipses.add(new Eclipse(missedEclipseWindow.getLeft(), missedEclipseWindow.getRight(),
                        OccultationType.PARTIAL, 0.0));
            }

            else {
                Eclipse prevEclipse = eclipses.get(i - 1);
                Duration difference = eclipseStart.absoluteDifference(prevEclipse.getEnd());
                OccultationType previousEclipseEntryType = prevEclipse.getEclipseType();

                //we know a partial eclipse entry is missing if there wasn't a partial eclipse in the index directly before the full eclipse
                //within a second of this full eclipse  (at index i-1 there may be a partial eclipse but that may be associated with a full
                //eclipse at index i-2 as a partial eclipse exit)
                if (eclipseType == OccultationType.FULL && !(previousEclipseEntryType == OccultationType.PARTIAL && difference.lessThan(minDurationBtwnEclipses))) {

                    List<Pair<Time, Time>> missedEclipses = Occultations.occultations(OccultationType.PARTIAL,
                            frontBody, Shape.ELLIPSOID, frontFrame, backBody, Shape.ELLIPSOID, backFrame, ABCORR.CN,
                            observer, new Duration("00:00:00.5"), eclipseStart.subtract(new Duration("00:05:00")),
                            eclipseStart.add(new Duration("00:05:00")));

                    // TODO: compute correct fracSunVisible
                    Pair<Time, Time> missedEclipseWindow = missedEclipses.get(0);
                    eclipses.add(new Eclipse(missedEclipseWindow.getLeft(), missedEclipseWindow.getRight(),
                            OccultationType.PARTIAL, 0.0));
                }
            }

            //this is the last index in the list; we know that a partial eclipse exit is missing
            if (i == numEclipses - 1) {
                List<Pair<Time, Time>> missedEclipses = Occultations.occultations(OccultationType.PARTIAL, frontBody,
                        Shape.ELLIPSOID, frontFrame, backBody, Shape.ELLIPSOID, backFrame, ABCORR.CN, observer,
                        new Duration("00:00:00.5"), eclipseEnd.subtract(new Duration("00:05:00")),
                        eclipseEnd.add(new Duration("00:05:00")));
                
                // TODO: compute correct fracSunVisible
                Pair<Time, Time> missedEclipseWindow = missedEclipses.get(0);
                eclipses.add(new Eclipse(missedEclipseWindow.getLeft(), missedEclipseWindow.getRight(),
                        OccultationType.PARTIAL, 0.0));
            }

            else {
                Eclipse nextEclipse = eclipses.get(i + 1);
                Duration difference = eclipseEnd.absoluteDifference(nextEclipse.getStart());
                OccultationType exitType = nextEclipse.getEclipseType();

                //we know a partial eclipse exit is missing if there wasn't a partial eclipse in the index directly after the full eclipse
                //within a second of this full eclipse  (at index i+1 there may be a partial eclipse but that may be associated with a full
                //eclipse at index i-+ as a partial eclipse entry)
                if (eclipseType == OccultationType.FULL && !(exitType == OccultationType.PARTIAL && difference.lessThan(minDurationBtwnEclipses))) {

                    List<Pair<Time, Time>> missedEclipses = Occultations.occultations(OccultationType.PARTIAL,
                            frontBody, Shape.ELLIPSOID, frontFrame, backBody, Shape.ELLIPSOID, backFrame, ABCORR.CN,
                            observer, new Duration("00:00:00.5"), eclipseStart.subtract(new Duration("00:05:00")),
                            eclipseStart.add(new Duration("00:05:00")));

                    // TODO: compute correct fracSunVisible
                    Pair<Time, Time> missedEclipseWindow = missedEclipses.get(0);
                    eclipses.add(new Eclipse(missedEclipseWindow.getLeft(), missedEclipseWindow.getRight(),
                            OccultationType.PARTIAL, 0.0));
                }
            }
        }
        eclipses.sort((a, b) -> a.getStart().compareTo(b.getStart()));
        return eclipses;
    }
}
