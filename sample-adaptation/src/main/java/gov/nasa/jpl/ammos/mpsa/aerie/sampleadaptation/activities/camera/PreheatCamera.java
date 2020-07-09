package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.camera;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.batteryCapacity;

@ActivityType(name="PreheatCamera", generateMapper=true)
public class PreheatCamera implements Activity {

    @Parameter
    public long heatDurationInSeconds;

    @Override
    public void modelEffects() {
        double totalEnergyUsed = heatDurationInSeconds * Config.cameraHeaterPower;
        batteryCapacity.add(-totalEnergyUsed);
        ctx.delay(Duration.of(heatDurationInSeconds, TimeUnit.SECONDS));
    }
}
