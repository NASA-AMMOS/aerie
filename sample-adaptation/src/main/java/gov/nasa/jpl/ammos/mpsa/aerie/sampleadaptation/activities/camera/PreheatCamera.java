package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEffects.delay;

public class PreheatCamera implements Activity {

    private static final double powerConsumptionRate = 100;

    @Parameter
    public Duration heatDuration;

    @Override
    public void modelEffects() {
        final var states = SampleMissionStates.getModel();
        final double heatDurationInSeconds = heatDuration.durationInMicroseconds*1000000;
        double totalPowerUsed = heatDurationInSeconds * powerConsumptionRate;
        states.cameraPower.set(totalPowerUsed);
        delay(heatDuration);
    }
}
