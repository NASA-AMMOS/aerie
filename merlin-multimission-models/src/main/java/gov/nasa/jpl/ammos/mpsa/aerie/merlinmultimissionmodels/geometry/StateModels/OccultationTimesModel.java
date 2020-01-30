package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.*;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.spicewrappers.Occultations;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.LazyEvaluator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import org.apache.commons.lang3.tuple.Pair;
import spice.basic.SpiceErrorException;

import java.util.List;
import java.util.Map;

/**
 * This state model stores the times at which occultations occur in a List
 *
 * References for this state model include:
 *      The multimission blackbird geometry model: https://github.jpl.nasa.gov/Blackbird/MultiMissionModels/
 *      The blackbird spice wrappers: https://github.jpl.nasa.gov/Blackbird/Blackbird/blob/master/src/main/java/gov/nasa/jpl/spice/Spice.java
 *      And the NAIF Spice method gfoclt_c.html
 */

public class OccultationTimesModel implements State<List<Pair<Time, Time>>> {

    private OccultationType occType = OccultationType.ANY;
    private Body frontBody;
    private Shape frontShape = Shape.ELLIPSOID;
    private ReferenceFrame frontFrame;
    private Body backBody;
    private Shape backShape = Shape.ELLIPSOID;
    private ReferenceFrame backFrame;
    private ABCORR abcorr = ABCORR.LT;
    private Body observer;
    private Duration stepSize;
    private Time start;
    private Time end;

    private final LazyEvaluator<List<Pair<Time, Time>>> evaluator = LazyEvaluator.of(() -> {

        List<Pair<Time, Time>> occultations = null;
        try {
            occultations = Occultations.occultations(occType, frontBody, frontShape, frontFrame, backBody, backShape,
                    backFrame, abcorr, observer, stepSize, start, end);
        } catch (SpiceErrorException e) {
            e.printStackTrace();
        }
        return occultations;
    });

    OccultationTimesModel(){}

    public OccultationTimesModel(Time start, Time end, Duration stepSize){
        this.start = start;
        this.end = end;
        this.stepSize = stepSize;
    }

    public void setOccType(OccultationType occType){
        this.occType = occType;
        evaluator.invalidate();
    }

    public void setFrontBody(Body frontBody) {
        this.frontBody = frontBody;
        evaluator.invalidate();
    }

    public void setFrontShape(Shape frontShape){
        this.frontShape = frontShape;
        evaluator.invalidate();
    }

    public void setFrontFrame(ReferenceFrame frontFrame) {
         this.frontFrame = frontFrame;
        evaluator.invalidate();
    }

    public void setBackBody(Body backBody){
        this.backBody = backBody;
        evaluator.invalidate();
    }

    public void setBackShape(Shape backShape){
        this.backShape = backShape;
        evaluator.invalidate();
    }

    public void setBackFrame(ReferenceFrame backFrame){
        this.backFrame = backFrame;
        evaluator.invalidate();
    }

    public void setAbcorr(ABCORR abcorr){
        this.abcorr = abcorr;
        evaluator.invalidate();
    }

    public void setObserver(Body observer){
        this.observer = observer;
        evaluator.invalidate();
    }

    public void setStepSize(Duration stepSize){
        this.stepSize = stepSize;
        evaluator.invalidate();
    }

    public void setStart(Time start){
        this.start = start;
        evaluator.invalidate();
    }

    public void setEnd(Time end){
        this.end = end;
        evaluator.invalidate();
    }

    @Override
    public List<Pair<Time, Time>> get() {
        return this.evaluator.get();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Map getHistory() {
        return null;
    }

    @Override
    public void setEngine(SimulationEngine engine) {
    }
}
