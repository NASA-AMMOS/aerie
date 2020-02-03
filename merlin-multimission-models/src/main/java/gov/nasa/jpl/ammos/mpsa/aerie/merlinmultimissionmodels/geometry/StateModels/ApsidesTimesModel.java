package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Apsis;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.Body;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers.Apsides;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.LazyEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import spice.basic.SpiceErrorException;

import java.util.List;
import java.util.Map;


/**
 * This state model stores the times at which apsides occurs in a List
 *
 * References for this state model include:
 *      The multimission blackbird geometry model: https://github.jpl.nasa.gov/Blackbird/MultiMissionModels/blob/e50f667e779664aa4fca82d94788ffe1c47ad600/blackbird-geometrymodel/src/main/java/gov/nasa/jpl/geometrymodel/AddApoapsis.java
 *      The blackbird spice wrappers: https://github.jpl.nasa.gov/Blackbird/Blackbird/blob/master/src/main/java/gov/nasa/jpl/spice/Spice.java
 *      And the NAIF Spice method gfdist: https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/FORTRAN/spicelib/gfdist.html
 */

public class ApsidesTimesModel implements State<List<Time>> {

    private Duration stepSize = Duration.fromMinutes(1);
    private Time start;
    private Time end;
    private double filter;
    private Body target;
    private Body observer;
    private Apsis apsisType;

    private final LazyEvaluator<List<Time>> evaluator = LazyEvaluator.of(() -> {

        List<Time> apsides = null;
        List<Time> filteredApsides = null;
        try {
            apsides = Apsides.apsides(target, observer, apsisType, stepSize, start, end);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }

        try {
            filteredApsides = Apsides.apsidesFilter(apsisType, filter, apsides, target, observer);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        return filteredApsides;
    });

    // TODO: Discuss different types of constructors and state model
    // initialization/configuration
    public ApsidesTimesModel() {
    }

    public ApsidesTimesModel(Time start, Time end, Duration stepSize) {
        this.start = start;
        this.end = end;
        this.stepSize = stepSize;
    }

    public void setStepSize(Duration stepSize) {
        this.stepSize = stepSize;
        this.evaluator.invalidate();
    }

    public void setStart(Time start) {
        this.start = start;
        this.evaluator.invalidate();
    }

    public void setEnd(Time end) {
        this.end = end;
        this.evaluator.invalidate();
    }

    public void setFilter(double filter) {
        this.filter = filter;
        this.evaluator.invalidate();
    }

    public void setTarget(Body target) {
        this.target = target;
        this.evaluator.invalidate();
    }

    public void setObserver(Body observer) {
        this.observer = observer;
        this.evaluator.invalidate();
    }

    public void setApsisType(Apsis apsisType) {
        this.apsisType = apsisType;
        this.evaluator.invalidate();
    }

    @Override
    public List<Time> get() {
        return this.evaluator.get();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Map<Instant, List<Time>> getHistory() {
        return null;
    }

    @Override
    public void setEngine(SimulationEngine engine) {
    }
}
