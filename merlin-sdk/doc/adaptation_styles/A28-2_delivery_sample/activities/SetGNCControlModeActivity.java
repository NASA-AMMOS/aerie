package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;

import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.GNCControlMode;
import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

/**
 * Sets the GNC Control Authority
 *
 * This activity sets the GNC control authority of the spacecraft to either RCS or RWA
 * (thrusters or reaction wheels)
 *
 * @subsystem GNC
 * @version 3.1.4
 * @contacts jdoe, fbar
 * @stakeholders GNC
 * @labels gnc
 * @dateCreated 2019-07-30
 * @dateLastModified 2019-07-30
 * @refs https://example.com/SetGNCControlMode
 */
@ActivityType("SetGNCControlMode")
public class SetGNCControlModeActivity implements Activity {

    @Parameter
    GNCControlMode newControlMode = GNCControlMode.RWA;

    public SetGNCControlModeActivity() {
    }

    public SetGNCControlModeActivity(String newControlMode) {
        this.newControlMode = newControlMode;
    }

    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        ClipperStates clipper = ctx.getStates();
        clipper.gnc.controlMode.set(newControlMode);
    }

}
