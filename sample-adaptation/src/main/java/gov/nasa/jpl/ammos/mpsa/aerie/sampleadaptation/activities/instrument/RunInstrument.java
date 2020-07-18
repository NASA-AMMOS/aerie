package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.SECONDS;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.duration;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.batteryCapacity;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.instrumentDataBits;

@ActivityType(name="RunInstrument", generateMapper=true)
public class RunInstrument implements Activity {

    public enum DataMode {
        LOW(250),
        MED(500),
        HIGH(1000);

        public final int bitsPerSecond;
        DataMode(int bitsPerSecond) {
            this.bitsPerSecond = bitsPerSecond;
        }
    }

    @Parameter
    public long durationInSeconds;

    @Parameter
    public DataMode dataMode = DataMode.MED;

    @Override
    public List<String> validateParameters() {
        final List<String> failures = new ArrayList<>();
        if (durationInSeconds < 0) {
            failures.add("`durationInSeconds` must be non-negative");
        }
        return failures;
    }

    public void modelEffects() {
        ctx.spawn(new TurnInstrumentOn());

        // Generate data and use power all at the start
        final double dataGenerated = dataMode.bitsPerSecond * durationInSeconds;
        final double powerConsumed = Config.instrumentPower * durationInSeconds;
        instrumentDataBits.add(dataGenerated);
        batteryCapacity.add(-powerConsumed);

        ctx.spawnAfter(duration(durationInSeconds, SECONDS), new TurnInstrumentOff());
    }
}
