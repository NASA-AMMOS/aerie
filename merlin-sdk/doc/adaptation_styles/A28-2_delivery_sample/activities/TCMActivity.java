package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.validation.ValidationResult;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.classes.Vector3D;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import gov.nasa.jpl.ammos.mpsa.merlin.multimissionmodels.gnc.classes.Attitude;

import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.AttitudeMode;
import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.GNCControlMode;
import gov.nasa.jpl.europa.clipper.merlin.someinstrument.classes.Enums.SomeInstrumentOpMode;
import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

/**
 * Performs a trajectory correction maneuver (TCM)
 * 
 * This maneuver primarily consists of a series of three TCM stages: prep, burn, and cleanup. The effect model also
 * handles GNC control authority switches to thrusters for the maneuver and to finalGNCControlMode if applicable. Upon
 * completion of the maneuver, it will also perform UVS and SUDA decontamination activities if specified by the
 * activity parameters.
 *
 * @subsystem GNC
 * @version 0.0.4
 * @contacts jonny.appleseed@jpl.nasa.gov
 * @stakeholders GNC, Propulsion
 * @labels gnc, prop, nav
 * @refs https://madeuplink.com/TCM
 */
@ActivityType("TCM")
public class TCMActivity implements Activity {

    public TCMActivity() {
    }

    /* ----------------------------- PARAMETER TYPES ---------------------------- */

    // create some enums just to be used within this activity
    // these could easily be located elsewhere (like in GNCEnums)

    @ParameterType
    public enum AttitudeOptimization {
        Power, Comm
    }

    @ParameterType
    public enum PIMSState {
        SURVEY, MAGNETOSPHERIC, TRANSITION, IONOSPHERIC, OFF
    }

    /* ------------------------------- PARAMETERS ------------------------------- */

    @Parameter
    String tcmId = "";

    @Parameter
    Boolean preFireHeatersRequired = false;

    @Parameter
    Duration burnDuration = Duration(1, Time.Minute);

    @Parameter
    Vector3D burnDirection = new Vector3D(1.0, 0.0, 0.0);

    @Parameter
    Double burnMagnitude = 0.0;

    @Parameter
    AttitudeOptimization attitudeOptimization = AttitudeOptimization.Comm;

    @Parameter
    Double solarArrayStowAngle;

    @Parameter
    GNCControlMode finalGNCControlMode = GNCControlMode.RWA;

    @Parameter
    AttitudeMode finalAttitudeMode = AttitudeMode.INERTIAL;

    @Parameter
    Double finalSAAngle = 0.0;

    @Parameter
    Attitude finalAttitude = new Attitude();

    @Parameter
    Boolean scheduleRhbModes = false;

    @Parameter
    Boolean sudaDecontaminationRequired = false;

    @Parameter
    Boolean uvsDecontaminationRequired = false;

    @Parameter
    PIMSState pimsState = PIMSState.OFF;

    /* ------------------------------- VALIDATION ------------------------------- */

    public List<ValidationResult> validateParameters() {
        List<ValidationResult> results = new ArrayList<>();

        if (finalSAAngle > 175 && finalSAAngle < 180) {
            results.add(ValidationResult.failure(
                    "The final solar array angle '" + finalSAAngle + "' is within the hard stop range [175, 180]."));
        }

        if (burnMagnitude < 0.0) {
            results.add(ValidationResult.failure(
                "The burn magnitude '" + burnMagnitude + "' must be non-negative."));
        }

        if (pimsState != PIMSState.OFF) {
            results.add(ValidationResult.warning(
                "The PIMS state '" + pimsState.toString() + "' should be OFF during a TCM"));
        }

        return results;
    }

    /* --------------------- EFFECT MODELING & DECOMPOSITION -------------------- */
    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        clipper = ctx.getStates();

        // modify some states at the beginning of a TCM
        clipper.gnc.tcmInProgress.setValue(true);
        clipper.gnc.tcmName.setValue(tcmId);
        clipper.gnc.lastTCMExecuted.setValue(ctx.now());

        // switch GNC control mode to RCS if we're currently running on RWA
        if (clipper.gnc.GNCControlMode == GNCControlMode.RWA) {
            ctx.callActivity(
                new SetGNCControlModeActivity(GNCControlMode.RCS)
            );
        }

        // prepare for the upcoming burn
        TCMPrepActivity prep = new TCMPrepActivity();
        {
            prep.tcmId = this.tcmId;
            prep.scheduleRhbModes = this.scheduleRhbModes;
            prep.preFireHeatersRequired = this.preFireHeatersRequired;
            prep.solarArrayStowAngle = this.solarArrayStowAngle;
        }
        ctx.callActivity(prep);

        // perform the maneuver
        TCMBurnActivity burn = new TCMBurnActivity();
        {
            burn.tcmId = this.tcmId;
            burn.commandedDuration = this.burnDuration;
            burn.direction = this.burnDirection;
            burn.magnitude = this.burnMagnitude;
            burn.scheduleRhbModes = this.scheduleRhbModes;
            burn.attitudeOptimization = this.attitudeOptimization;
            burn.pimsState = this.pimsState;
            burn.finalAttitudeMode = this.finalAttitudeMode;
            burn.finalSAAngle = this.finalSAAngle;
            burn.finalAttitude = this.finalAttitude;
        }
        ctx.callActivity(burn);

        TCMCleanupActivity cleanup = new TCMCleanupActivity(this.tcmId);
        ctx.callActivity(cleanup);

        clipper.gnc.tcmInProgress.setValue(false);

        // switch from RCS to RWA if necessary
        if (finalGNCControlMode == finalGNCControlMode.RWA) {
            ctx.callActivity(
                new SetGNCControlModeActivity(GNCControlMode.RWA)
            );
        }

        // perform UVS & SUDA decontaminations if necessary
        if (uvsDecontaminationRequired) {
            ctx.callActivity(
                new UVSDecontaminationActivity()
            );
        }
        if (sudaDecontaminationRequired) {
            ctx.callActivity(
                new SUDADecontaminationActivity()
            );
        }

    }

}