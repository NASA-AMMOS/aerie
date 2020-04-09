package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;


@ActivityType(name="TurnInstrumentOff", states=SampleMissionStates.class, generateMapper=true)
public class TurnInstrumentOff implements Activity {
    @Override
    public void modelEffects() {
        final var states = SampleMissionStates.getModel();
        states.instrumentData.turnOff();
        states.instrumentPower_W.set(0.0);
    }
}
