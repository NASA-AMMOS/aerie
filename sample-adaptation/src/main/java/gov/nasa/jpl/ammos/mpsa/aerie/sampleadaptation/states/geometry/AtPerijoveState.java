package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.geometry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels.ApsidesTimesModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.DerivedState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

public class AtPerijoveState extends DerivedState<Boolean> {

    private SimulationEngine engine;
    private List<Time> perijoveTimes;
    private Map<Instant, Boolean> stateHistory = new LinkedHashMap<>();

    private Time missionStartTime;
    private Instant simStartTime;

    public AtPerijoveState(Config config) {
        ApsidesTimesModel model = GeometryInitializer.initPerijoveTimesModel(config);
        perijoveTimes = model.get();
        missionStartTime = config.missionStartTime;
    }

    @Override
    public Boolean get() {
        for (Time time : perijoveTimes) {
            final Instant periJoveInstant = timeToInstant(time);

            Instant startPerijove = periJoveInstant.minus(1, TimeUnit.HOURS);
            Instant endPerijove = periJoveInstant.plus(1, TimeUnit.HOURS);
            Instant currentTime = engine.getCurrentSimulationTime();

            // if our current time is earlier than a given perijove in the list that we are
            // comparing to, then all perijoves are in the future; i.e., we are not at perijove
            if (currentTime.isBefore(startPerijove)) {
                return false;
            }

            // if we are within our window, exit loop and return
            // otherwise, keep searching until all perijoves are in the future
            if (!currentTime.isAfter(endPerijove)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setEngine(SimulationEngine engine) {
        simStartTime = engine.getCurrentSimulationTime();

        stateHistory.put(simStartTime, false);
        for (Time time : perijoveTimes) {
            final Instant perijoveInstant = timeToInstant(time);

            stateHistory.put(perijoveInstant.minus(1, TimeUnit.HOURS), true);
            // TODO: should this be endPerijove + some small delta??
            stateHistory.put(perijoveInstant.plus(1, TimeUnit.HOURS), false);
        }

        this.engine = engine;
    }

    private Instant timeToInstant(Time time) {
        return simStartTime.plus(time.minus(missionStartTime).getMicroseconds(), TimeUnit.MICROSECONDS);
    }

}