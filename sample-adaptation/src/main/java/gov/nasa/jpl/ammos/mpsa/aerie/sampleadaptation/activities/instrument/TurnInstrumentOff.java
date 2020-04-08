package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;


@ActivityType(name="TurnInstrumentOff", states=SampleMissionStates.class, generateMapper=true)
public class TurnInstrumentOff implements Activity<StateContainer> {
    @Override
    public void modelEffects(@Deprecated(forRemoval=true) StateContainer states){
        SampleMissionStates.getModel().instrumentData.turnOff();
        SampleMissionStates.getModel().instrumentPower_W.set(0.0);
    }
}
