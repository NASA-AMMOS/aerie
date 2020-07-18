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

/**
 * Performs a someInstrument calibration
 *
 * The instrument calibration activity requires that the spacecraft perform a scan provided a specified
 * scanStartAttitude and scanFinalAttitude with the instrument in a specified operational mode. To achieve this,
 * the instrument warmup and slew to scanStart occur in parallel. Once the slew to scanStart is complete, the
 * spacecraft will switch its GNC control authority to reaction wheels if it is currently using thrusters. After
 * warmup, slew to scanStart, and the control authority switchover are complete, we proceed with the calibration. The
 * activity sets the instrument's operational mode to a calibration setting, performs the scan, and turns the
 * instrument off. To conclude, it slews to an Earth-pointing attitude for downlink.
 *
 * @subsystem SomeInstrument
 * @version 1.0.0
 * @contacts jdoe
 * @stakeholders GNC, SomeInstrument
 * @labels gnc, someinstrument
 * @dateCreated 2019-07-30
 * @dateLastModified 2019-07-30
 * @refs https://example.com/SomeInstrumentCalibration
 */
@ActivityType("SomeInstrumentCalibration")
public class SomeInstrumentCalibrationActivity implements Activity {

    public SomeInstrumentCalibrationActivity() {
    }

    /* ------------------------------- PARAMETERS ------------------------------- */

    @Parameter
    Duration instrumentWarmupDuration = Duration.of(2, Duration.HOURS);

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

        if (instrumentWarmupDuration.shorterThan(Duration.of(1, Duration.HOURS))
                || instrumentWarmupDuration.longerThan(Duration.of(3, Duration.HOURS))) {
            results.add(ActivityValidation.failure("The instrument warmup duration '" + instrumentWarmupDuration
                    + "' is not within the range [1, 3] hours."));
        }

        return results;
    }

    /* --------------------- EFFECT MODELING & DECOMPOSITION -------------------- */
    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        ClipperStates clipper = ctx.getStates();

        // mark the calibration as in-progress
        clipper.instruments.someInstrument.calibrationInProgress.set(true);

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

        clipper.instruments.someInstrument.calibrationInProgress.set(false);

        // change attitude to Earth-pointing after the calibration
        ctx.callActivity(
            new SetAttitudeModeActivity(AttitudeMode.EARTH_POINTING)
        );

    }

}
