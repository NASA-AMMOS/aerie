package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import java.util.function.Function;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.DownlinkData;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.InitializeBinDataVolume;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOff;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOn;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.deferTo;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.now;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.spawn;

public class Plan {
    public static void runPlan(Config config) {
        final var startTime = now();

        final Function<Time, Instant> timeToInstant = (Time time) ->
            startTime.plus(time.minus(config.missionStartTime).getMicroseconds(), TimeUnit.MICROSECONDS);

        // initialize data volume at mission start
        spawn(new InitializeBinDataVolume());

        for (Time periapseTime : Geometry.getPeriapsides(config)) {
            TurnInstrumentOn turnInstrumentOnActivity = new TurnInstrumentOn();
            TurnInstrumentOff turnInstrumentOffActivity = new TurnInstrumentOff();

            deferTo(timeToInstant.apply(periapseTime).minus(1, TimeUnit.HOURS), turnInstrumentOnActivity);
            deferTo(timeToInstant.apply(periapseTime).plus(1, TimeUnit.HOURS), turnInstrumentOffActivity);
        }

        for (Time apoapseTime : Geometry.getApoapsides(config)) {
            DownlinkData downlinkActivity = new DownlinkData();
            downlinkActivity.downlinkAll = true;

            deferTo(timeToInstant.apply(apoapseTime).minus(1, TimeUnit.HOURS), downlinkActivity);
        }
    }
}