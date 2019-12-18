package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;


@ActivityType(name="TurnInstrumentOff", states=SampleMissionStates.class)
public class TurnInstrumentOff implements Activity<SampleMissionStates> {

    @Override
    public void modelEffects(SimulationContext ctx, SampleMissionStates states){
        states.instrumentData.turnOff();
        states.instrumentPower_W.set(0.0);
    }
}
