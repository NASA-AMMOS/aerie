package gov.nasa.jpl.europa.clipper.merlin.gnc.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.simulation.annotations.SimulationContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import gov.nasa.jpl.europa.clipper.merlin.states.ClipperStates;

/**
 * Sets the spacecraft scan rate
 *
 * @subsystem GNC
 * @version 0.5.1
 * @contacts john.doe@jpl.nasa.gov
 * @stakeholders GNC
 * @labels gnc
 * @dateCreated 2019-07-30
 * @dateLastModified 2019-07-30
 * @refs https://madeuplink.com/SetScanAxis
 */
@ActivityType("SetScanRate")
public class SetScanRateActivity implements Activity {

    @Parameter
    Double newScanRate = 1.0;

    public SetScanRateActivity() {
    }

    @SimulationContext
    Context<ClipperStates> ctx;

    public void modelEffects() {
        clipper = ctx.getStates();
        clipper.gnc.scanRate.set(newScanRate);
    }

}