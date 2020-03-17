package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.geometry;

import java.util.List;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.StateModels.ApsidesTimesModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.DerivedState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

public class AtApojoveState extends DerivedState<Boolean> {
    private List<Time> apojoveTimes;

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
            Instant currentTime = SimulationEffects.now();

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
    public void initialize(final Instant startTime) {
        this.simStartTime = startTime;

        this.stateHistory.put(this.simStartTime, false);
        for (final var time : this.apojoveTimes) {
            final Instant apojoveInstant = timeToInstant(time);

            this.stateHistory.put(apojoveInstant.minus(1, TimeUnit.HOURS), true);
            this.stateHistory.put(apojoveInstant.plus(1, TimeUnit.HOURS), false);
        }
    }

    private Instant timeToInstant(Time time) {
        return simStartTime.plus(time.minus(missionStartTime).getMicroseconds(), TimeUnit.MICROSECONDS);
    }
}