package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.OnboardDataModelStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;

import java.util.ArrayList;
import java.util.List;


/**
 * DownlinkData an amount of data from a specific bin.
 *
 * @subsystem Data
 * @contact mkumar
 */

@ActivityType("DownlinkData")
public class DownlinkData extends Activity<OnboardDataModelStates> {

    @Parameter
    public String binID = "";

  //  @Parameter
    public boolean downlinkAll = true;

    @Parameter
    public double downlinkAmount = 0.0;


    @Override
    public List<String> validateParameters() {
        final List<String> failures = new ArrayList<>();

        if (this.downlinkAmount <= 0  && !downlinkAll) {
            failures.add("downlinked amount must be positive");
        }

        return failures;
    }

    @Override
    public void modelEffects(SimulationContext ctx, OnboardDataModelStates states){
        if (downlinkAll){
            states.getBinByName(binID).downlink();
        }
        else {
            states.getBinByName(binID).downlink(downlinkAmount);
        }
    }


}
