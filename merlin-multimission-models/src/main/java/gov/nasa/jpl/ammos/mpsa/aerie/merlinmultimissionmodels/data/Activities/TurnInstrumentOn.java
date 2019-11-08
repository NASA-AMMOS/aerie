package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.Activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.InstrumentModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels.OnboardDataModelStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;

import java.util.ArrayList;
import java.util.List;


@ActivityType("TurnInstrumentOn")
public class TurnInstrumentOn extends Activity<OnboardDataModelStates> {

    @Parameter
    public String instrumentName = "";


    @Parameter
    public double instrumentRate = 0.0;


    @Override
    public List<String> validateParameters() {
        final List<String> failures = new ArrayList<>();
        if (instrumentRate <= 0.0){
            failures.add("data rate must be positive and greater than 0");
        }
        return failures;
    }

    @Override
    public void modelEffects(SimulationContext ctx, OnboardDataModelStates states){
        InstrumentModel instrument = states.getInstrumentByName(instrumentName);
        instrument.turnOn(instrumentRate);
    }
}
