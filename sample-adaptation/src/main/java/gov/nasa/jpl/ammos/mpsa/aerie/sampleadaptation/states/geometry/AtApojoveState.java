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

public class AtApojoveState extends DerivedState<Boolean> {

    private SimulationEngine engine;
    private List<Time> apojoveTimes;
    private Map<Instant, Boolean> stateHistory = new LinkedHashMap<>();

    private Time missionStartTime;
    private Instant simStartTime;

    public AtApojoveState(Config config) {
        ApsidesTimesModel model = GeometryInitializer.initApojoveTimesModel(config);
        apojoveTimes = model.get();
        missionStartTime = config.missionStartTime;
    }

    @Override
    public Boolean get() {
        for (Time time : apojoveTimes) {
            final Instant apojoveInstant = timeToInstant(time);

            Instant startApojove = apojoveInstant.minus(1, TimeUnit.HOURS);
            Instant endApojove = apojoveInstant.plus(1, TimeUnit.HOURS);
            Instant currentTime = engine.getCurrentSimulationTime();

            // if our current time is earlier than a given apojove in the list that we are
            // comparing to, then all apojoves are in the future; i.e., we are not at apojove
            if (currentTime.isBefore(startApojove)) {
                return false;
            }

            // if we are within our window, exit loop and return
            // otherwise, keep searching until all apojoves are in the future
            if (!currentTime.isAfter(endApojove)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setEngine(SimulationEngine engine) {
        simStartTime = engine.getCurrentSimulationTime();

        stateHistory.put(simStartTime, false);
        for (Time time : apojoveTimes) {
            final Instant apojoveInstant = timeToInstant(time);

            stateHistory.put(apojoveInstant.minus(1, TimeUnit.HOURS), true);
            // TODO: should this be endApojove + some small delta??
            stateHistory.put(apojoveInstant.plus(1, TimeUnit.HOURS), false);
        }

        this.engine = engine;
    }

    private Instant timeToInstant(Time time) {
        return simStartTime.plus(time.minus(missionStartTime).getMicroseconds(), TimeUnit.MICROSECONDS);
    }

}