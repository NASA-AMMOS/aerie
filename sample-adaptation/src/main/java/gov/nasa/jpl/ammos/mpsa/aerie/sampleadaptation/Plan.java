package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.ActivityJob;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.DownlinkData;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.InitializeBinDataVolume;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOff;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOn;

public class Plan {
    
    public static List<ActivityJob<?>> createPlan( Config config ) {
        
        List<ActivityJob<?>> activityList = new ArrayList<>();

        // initialize data volume at mission start
        InitializeBinDataVolume initBinsActivity = new InitializeBinDataVolume();
        activityList.add(new ActivityJob<>(
            initBinsActivity,
            config.missionStartTime
        ));

        Duration oneHour = Duration.fromHours(1);

        Geometry.loadSpiceAndKernels();
        List<Time> periapsidesTimes = Geometry.getPeriapsides(config);
        
        for (Time periapseTime : periapsidesTimes) {
            TurnInstrumentOn turnInstrumentOnActivity = new TurnInstrumentOn();
            TurnInstrumentOff turnInstrumentOffActivity = new TurnInstrumentOff();
            
            activityList.add(new ActivityJob<>(
                turnInstrumentOnActivity,
                periapseTime.subtract(oneHour)
            ));

            activityList.add(new ActivityJob<>(
                turnInstrumentOffActivity,
                periapseTime.add(oneHour)
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
                apoapseTime.subtract(oneHour)
            ));
        }

        return activityList;
    }

}