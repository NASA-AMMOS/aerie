package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;

import gov.nasa.jpl.ammos.mpsa.merlin.multimissionmodels.gnc.classes.Attitude;

import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.AttitudeMode;
import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

@ActivityType("SetAttitudeMode")
public class SetAttitudeModeActivity implements Activity {

    @Parameter
    AttitudeMode newAttitudeMode;

    public SetAttitudeModeActivity() {
    }

    public SetAttitudeModeActivity(AttitudeMode newAttitudeMode) {
        this.newAttitudeMode = newAttitudeMode;
    }

    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        Attitude newBaseAttitude;
        // retrieve the proper base attitude based on the newAttitudeMode
        // call some external PointingUtils library
        switch (newAttitudeMode) {
        case EARTH1:
            newBaseAttitude = PointingUtils.getEarthPointAttitude();
            break;
        case SUN:
            newBaseAttitude = PointingUtils.getSunPointAttitude();
            break;
        }
        
        CommandAttitudeActivity cmdAttitude = new CommandAttitudeActivity();
        {
            cmdAttitude.newBaseAttitude = newBaseAttitude;
            cmdAttitude.newBaseAttitudeMode = this.newAttitudeMode;
        }
        ctx.callActivity(cmdAttitude);
    }

}