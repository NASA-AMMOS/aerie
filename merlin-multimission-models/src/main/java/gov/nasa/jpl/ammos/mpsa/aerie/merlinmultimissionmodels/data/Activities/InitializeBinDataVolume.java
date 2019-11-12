package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.BinModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.OnboardDataModelStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;

import java.util.List;


//you *could* say we should do the initializing of the timeLastUpdate() in the class (check if null and if so update)
//but this would only occur when the first instrument is turned on or another action which triggers the integratoin
//so then you won't start recording until that point in time.  You won't get the (0,0) value.

@ActivityType(name="InitializeBinDataVolume", states=OnboardDataModelStates.class)
public class InitializeBinDataVolume implements Activity<OnboardDataModelStates> {
    //this activitiy initializes the state history of each bin with the current sim time and total data rate
    @Override
    public void modelEffects(SimulationContext ctx, OnboardDataModelStates states){

        List<BinModel> bins = states.getBinModelList();
        for (BinModel x : bins){
            x.initializeBinData();
        }
    }
}
