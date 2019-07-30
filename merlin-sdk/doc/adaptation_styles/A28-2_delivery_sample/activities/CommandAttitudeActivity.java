package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;

import gov.nasa.jpl.ammos.mpsa.merlin.multimissionmodels.gnc.classes.Attitude;

import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.AttitudeMode;
import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

@ActivityType("CommandAttitude")
public class CommandAttitudeActivity implements Activity {

    @Parameter
    AttitudeMode newBaseAttitudeMode;

    @Parameter
    Attitude newBaseAttitude;

    public CommandAttitudeActivity() {
    }

    public CommandAttitudeActivity(AttitudeMode newBaseAttitudeMode, Attitude newBaseAttitude) {
        this.newBaseAttitudeMode = newBaseAttitudeMode;
        this.newBaseAttitude = newBaseAttitude;
    }

    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        // FIXME
    }

}