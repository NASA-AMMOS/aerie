package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.classes.Vector3D;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

@ActivityType("SetScanAxis")
public class SetScanAxisActivity implements Activity {

    @Parameter
    Vector3D newScanAxis = new Vector3D(1.0, 0.0, 0.0);

    public SetScanAxisActivity() {
    }

    public SetScanAxisActivity(Vector3D newScanAxis) {
        this.newScanAxis = newScanAxis;
    }

    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        clipper = ctx.getStates();
        clipper.gnc.scanAxis.set(newScanAxis);
        ctx.wait(1, Time.second);
    }

}