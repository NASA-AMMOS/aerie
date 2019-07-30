package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

/**
 * Sets the spacecraft roll angle
 *
 * @subsystem GNC
 * @version 1.2.3
 * @contacts john.doe@jpl.nasa.gov, foo.bar@jpl.nasa.gov
 * @stakeholders GNC
 * @labels gnc
 * @dateCreated 2019-07-30
 * @dateLastModified 2019-07-30
 * @refs https://example.com/SetRollAngle
 */
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
        ClipperStates clipper = ctx.getStates();
        clipper.gnc.rollAngle.set(newRollAngle);
    }

}
