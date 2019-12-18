package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.statemodels.data.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.List;


//you *could* say we should do the initializing of the timeLastUpdate() in the class (check if null and if so update)
//but this would only occur when the first instrument is turned on or another action which triggers the integratoin
//so then you won't start recording until that point in time.  You won't get the (0,0) value.

@ActivityType(name="InitializeBinDataVolume", states=SampleMissionStates.class)
public class InitializeBinDataVolume implements Activity<SampleMissionStates> {
    //this activitiy initializes the state history of each bin with the current sim time and total data rate
    @Override
    public void modelEffects(SimulationContext ctx, SampleMissionStates states){

        List<BinModel> bins = states.getBinModelList();
        for (BinModel x : bins){
            x.initializeBinData();
        }
    }
}
