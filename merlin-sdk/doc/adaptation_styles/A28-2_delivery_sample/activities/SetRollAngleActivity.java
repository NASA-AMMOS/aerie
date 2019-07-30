package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

@ActivityType("SetRollAngle")
public class SetRollAngleActivity implements Activity {

    @Parameter
    Double newRollAngle = 0.0;

    public SetRollAngleActivity() {
    }

    public SetRollAngleActivity(Double newRollAngle) {
        this.newRollAngle = newRollAngle;
    }


    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        clipper = ctx.getStates();
        clipper.gnc.rollAngle.set(newRollAngle);
        ctx.wait(1, Time.second);
    }

}