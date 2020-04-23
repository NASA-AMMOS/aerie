package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.now;

//you *could* say we should do the initializing of the timeLastUpdate() in the class (check if null and if so update)
//but this would only occur when the first instrument is turned on or another action which triggers the integratoin
//so then you won't start recording until that point in time.  You won't get the (0,0) value.

@ActivityType(name="InitializeBinDataVolume", generateMapper=true)
public class InitializeBinDataVolume implements Activity {
    //this activity initializes the state history of each bin with the current sim time and total data rate
    @Override
    public void modelEffects() {
        final var states = SampleMissionStates.getModel();
        for (final var bin : states.allBins) bin.initialize(now());
    }
}
