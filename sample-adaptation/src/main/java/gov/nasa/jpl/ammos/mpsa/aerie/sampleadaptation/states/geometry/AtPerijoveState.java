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

public class AtPerijoveState extends DerivedState<Boolean> {

    private SimulationEngine engine;
    private List<Time> perijoveTimes;
    private Map<Time, Boolean> stateHistory = new LinkedHashMap<>();

    public AtPerijoveState(Config config) {
        ApsidesTimesModel model = GeometryInitializer.initPerijoveTimesModel(config);
        perijoveTimes = model.get();

        stateHistory.put(Time.MIN_TIME, false);
        for (Time time : perijoveTimes) {
            Time startPerijove = time.minus(Duration.fromHours(1.0));
            Time endPerijove = time.add(Duration.fromHours(1.0));
            stateHistory.put(startPerijove, true);
            // TODO: should this be endPerijove + some small delta??
            stateHistory.put(endPerijove, false);
        }
    }

    @Override
    public Boolean get() {
        for (Time time : perijoveTimes) {
            Time startPerijove = time.minus(Duration.fromHours(1.0));
            Time endPerijove = time.add(Duration.fromHours(1.0));
            Time currentTime = engine.getCurrentSimulationTime();

            // if our current time is earlier than a given perijove in the list that we are
            // comparing to, then all perijoves are in the future; i.e., we are not at perijove
            if (currentTime.lessThan(startPerijove)) {
                return false;
            }

            Boolean withinWindow = currentTime.greaterThanOrEqualTo(startPerijove)
                    && (currentTime.lessThanOrEqualTo(endPerijove));
            // if we are within our window, exit loop and return
            // otherwise, keep searching until all perijoves are in the future
            if (withinWindow) {
                return true;
            }
        }
        return false;
    }

}