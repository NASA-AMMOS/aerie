package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.DownlinkData;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.InitializeBinDataVolume;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOff;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOn;

public class Plan {
    
    public static List<ActivityJob<?>> createPlan( Config config, Instant simStartTime ) {
        final Function<Time, Instant> timeToInstant = (Time time) ->
            simStartTime.plus(time.minus(config.missionStartTime).getMicroseconds(), TimeUnit.MICROSECONDS);

        List<ActivityJob<?>> activityList = new ArrayList<>();

        // initialize data volume at mission start
        InitializeBinDataVolume initBinsActivity = new InitializeBinDataVolume();
        activityList.add(new ActivityJob<>(initBinsActivity, simStartTime));

        Duration oneHour = Duration.fromHours(1);

        Geometry.loadSpiceAndKernels();
        List<Time> periapsidesTimes = Geometry.getPeriapsides(config);
        
        for (Time periapseTime : periapsidesTimes) {
            TurnInstrumentOn turnInstrumentOnActivity = new TurnInstrumentOn();
            TurnInstrumentOff turnInstrumentOffActivity = new TurnInstrumentOff();

            activityList.add(new ActivityJob<>(
                turnInstrumentOnActivity,
                timeToInstant.apply(periapseTime).minus(1, TimeUnit.HOURS)
            ));

            activityList.add(new ActivityJob<>(
                turnInstrumentOffActivity,
                timeToInstant.apply(periapseTime).plus(1, TimeUnit.HOURS)
            ));
        }

        List<Time> apoapsidesTimes = Geometry.getApoapsides(config);
        
        for (Time apoapseTime : apoapsidesTimes) {
            DownlinkData downlinkActivity = new DownlinkData();
            {
                downlinkActivity.downlinkAll = true;
            }
            activityList.add(new ActivityJob<>(
                downlinkActivity,
                timeToInstant.apply(apoapseTime).minus(1, TimeUnit.HOURS)
            ));
        }

        return activityList;
    }

}