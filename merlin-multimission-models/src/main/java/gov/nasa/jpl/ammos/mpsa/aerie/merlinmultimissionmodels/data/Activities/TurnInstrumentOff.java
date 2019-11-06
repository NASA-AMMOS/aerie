package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.InstrumentModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.OnboardDataModelStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;

@ActivityType("TurnInstrumentOff")
public class TurnInstrumentOff implements Activity<OnboardDataModelStates> {

    @Parameter
    public String instrumentName = "";

    @Override
    public void modelEffects(SimulationContext<OnboardDataModelStates> ctx, OnboardDataModelStates states){
        InstrumentModel instrument = states.getInstrumentByName(instrumentName);
        instrument.turnOff();
    }
}
