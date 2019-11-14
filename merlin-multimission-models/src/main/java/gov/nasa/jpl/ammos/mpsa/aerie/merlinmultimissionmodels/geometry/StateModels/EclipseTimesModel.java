package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.classes.Eclipse;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.LazyEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import org.apache.commons.lang3.tuple.Pair;
import spice.basic.SpiceErrorException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers.Eclipses;

import java.util.List;
import java.util.Map;

public class EclipseTimesModel implements State<List<Eclipse>>{

    private Body frontBody;
    private ReferenceFrame frontFrame;
    private Body backBody;
    private ReferenceFrame backFrame;
    private Body observer;
    private Duration stepSize;
    private Time start;
    private Time end;

    private final LazyEvaluator<List<Eclipse>> evaluator = LazyEvaluator.of(() -> {

        List<Eclipse> eclipses = null;
        try {
            eclipses = Eclipses.allEclipses(frontBody, frontFrame, backBody, backFrame, observer, stepSize, start, end);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        return eclipses;
    });

    EclipseTimesModel() {
    }

    public EclipseTimesModel(Time start, Time end, Duration stepSize) {
        this.start = start;
        this.end = end;
        this.stepSize = stepSize;
    }

    public void setFrontBody(Body frontBody) {
        this.frontBody = frontBody;
        evaluator.invalidate();
    }

    public void setFrontFrame(ReferenceFrame frontFrame) {
        this.frontFrame = frontFrame;
        evaluator.invalidate();
    }

    public void setBackBody(Body backBody) {
        this.backBody = backBody;
        evaluator.invalidate();
    }

    public void setBackFrame(ReferenceFrame backFrame) {
        this.backFrame = backFrame;
        evaluator.invalidate();
    }

    public void setObserver(Body observer) {
        this.observer = observer;
        evaluator.invalidate();
    }

    public void setStepSize(Duration stepSize) {
        this.stepSize = stepSize;
        evaluator.invalidate();
    }

    public void setStart(Time start) {
        this.start = start;
        evaluator.invalidate();
    }

    public void setEnd(Time end) {
        this.end = end;
        evaluator.invalidate();
    }

    @Override
    public List<Eclipse> get() {
        return this.evaluator.get();
    }

    @Override
    public String getName() {
        return super.toString();
    }

    @Override
    public Map getHistory() {
        return null;
    }

    @Override
    public void setEngine(SimulationEngine engine) {
    }
}
