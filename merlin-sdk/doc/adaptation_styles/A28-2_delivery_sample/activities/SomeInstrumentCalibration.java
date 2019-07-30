package gov.nasa.jpl.europa.clipper.merlin.someinstrument.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.validation.ValidationResult;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.classes.Vector3D;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import gov.nasa.jpl.ammos.mpsa.merlin.multimissionmodels.gnc.classes.Attitude;

import gov.nasa.jpl.europa.clipper.merlin.gnc.classes.Enums.GNCControlMode;
import gov.nasa.jpl.europa.clipper.merlin.someinstrument.classes.Enums.SomeInstrumentOpMode;
import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

@ActivityType("SomeInstrumentCalibration")
public class SomeInstrumentCalibrationActivity implements Activity {

    public SomeInstrumentCalibrationActivity() {
    }

    /* ------------------------------- PARAMETERS ------------------------------- */

    @Parameter
    Duration instrumentWarmupDuration = Duration(2, Time.Hour);

    @Parameter
    Attitude scanStartAttitude = new Attitude();

    @Parameter
    Attitude scanFinalAttitude = new Attitude();

    @Parameter
    Double scanRate = 0.0;

    @Parameter
    Vector3D scanAxis = 0.0;

    /* ------------------------------- VALIDATION ------------------------------- */

    public List<ActivityValidationResult> validateParameters() {
        List<ActivityValidationResult> results = new ArrayList<>();

        if (scanRate < 0.0) {
            results.add(ActivityValidation.failure("The scan rate '" + scanRate + "' must be non-negative."));
        }

        if (instrumentWarmupDuration.lessThan(Duration(1, Time.Hour))
                || instrumentWarmupDuration.moreThan(Duration(3, Time.Hour))) {
            results.add(ActivityValidation.failure("The instrument warmup duration '" + instrumentWarmupDuration
                    + "' is not within the range [1, 3] hours."));
        }

        return results;
    }

    /* --------------------- EFFECT MODELING & DECOMPOSITION -------------------- */
    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        clipper = ctx.getStates();

        // mark the calibration as in-progress
        clipper.instruments.someInstrument.calibrationInProgress.setValue(true);

        // in parallel: warm up the instrument and change the attitude to the starting
        // scan attitude
        // NOTE: we want to capture one of the children (CommandAttitudeActivity) because
        //       we might need to change the GNC control mode right after we complete our
        //       slew, and we don't want to wait for all children to complete (in case
        //       warmup takes longer)
        ctx.spawnActivity(
            new SomeIntrumentWarmupActivity(this.instrumentWarmupDuration)
        );
        CommandAttitudeActivity cmdAttitude = ctx.spawnActivity(
            new CommandAttitudeActivity(AttitudeMode.INERTIAL, this.scanStartAttitude)
        );

        // if necessary: wait for attitude slew to finish, then switch GNC control
        // mode to RWA
        // NOTE: it is OK to switch GNC control modes even if the instrument is
        //       still warming up
        if (clipper.gnc.controlMode.getValue() != GNCControlMode.RWA) {
            ctx.waitForChild(cmdAttitude);
            ctx.spawnActivity(
                new SetGNCControlModeActivity(GNCControlMode.RWA)
            );
        }

        // wait for instrument warmup, attitude slew, and gnc control mode setting
        // to complete before continuing
        ctx.waitForAllChildren();

        // change the operational mode of the instrument for calibration
        ctx.callActivity(
            new SetSomeInstrumentOpModeActivity(SomeInstrumentOpMode.CALIBRATION)
        );

        // perform a scan while the instrument is in calibration mode
        PerformScanActivity scanActivity = new PerformScanActivity();
        {
            scanActivity.scanRate = this.scanRate;
            scanActivity.scanAxis = this.scanAxis;
            scanActivity.startAttitude = this.scanStartAttitude;
            scanActivity.finalAttitude = this.scanFinalAttitude;
        }
        ctx.callActivity(scanActivity);

        // put the instrument back in survival mode
        ctx.callActivity(
            new SetSomeInstrumentOpModeActivity(SomeInstrumentOpMode.SURVIVAL)
        );

        clipper.instruments.someInstrument.calibrationInProgress.setValue(false);

        // change attitude to Earth-pointing after the calibration
        ctx.callActivity(
            new SetAttitudeModeActivity(AttitudeMode.EARTH_POINTING)
        );

    }

}