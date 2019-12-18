package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.geometry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels.ApsidesTimesModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.DerivedState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

public class AtApojoveState extends DerivedState<Boolean> {

    private SimulationEngine engine;
    private List<Time> apojoveTimes;
    private Map<Time, Boolean> stateHistory = new LinkedHashMap<>();

    public AtApojoveState(Config config) {
        ApsidesTimesModel model = GeometryInitializer.initApojoveTimesModel(config);
        apojoveTimes = model.get();

        stateHistory.put(Time.MIN_TIME, false);
        for (Time time : apojoveTimes) {
            Time startApojove = time.minus(Duration.fromHours(1.0));
            Time endApojove = time.add(Duration.fromHours(1.0));
            stateHistory.put(startApojove, true);
            // TODO: should this be endApojove + some small delta??
            stateHistory.put(endApojove, false);
        }
    }

    @Override
    public Boolean get() {
        for (Time time : apojoveTimes) {
            Time startApojove = time.minus(Duration.fromHours(1.0));
            Time endApojove = time.add(Duration.fromHours(1.0));
            Time currentTime = engine.getCurrentSimulationTime();

            // if our current time is earlier than a given apojove in the list that we are
            // comparing to, then all apojoves are in the future; i.e., we are not at apojove
            if (currentTime.lessThan(startApojove)) {
                return false;
            }

            Boolean withinWindow = currentTime.greaterThanOrEqualTo(startApojove)
                    && (currentTime.lessThanOrEqualTo(endApojove));
            // if we are within our window, exit loop and return
            // otherwise, keep searching until all apojoves are in the future
            if (withinWindow) {
                return true;
            }
        }
        return false;
    }
}