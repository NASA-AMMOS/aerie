package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.ArrayList;
import java.util.List;


@ActivityType(name="TurnInstrumentOn", states=SampleMissionStates.class)
public class TurnInstrumentOn implements Activity<SampleMissionStates> {

    @Parameter
    public double instrumentRate = 10.0;

    /**
     * the bus power consumed by the instrument while it is turned on
     *
     * measured in Watts
     */
    @Parameter
    public double instrumentPower_W = 100.0;

    @Override
    public List<String> validateParameters() {
        final List<String> failures = new ArrayList<>();
        if (instrumentRate <= 0.0){
            failures.add("data rate must be positive and greater than 0");
        }
        return failures;
    }

    @Override
    public void modelEffects(SampleMissionStates states){
        states.instrumentData.turnOn(instrumentRate);
        states.instrumentPower_W.set(instrumentPower_W);
    }
}
