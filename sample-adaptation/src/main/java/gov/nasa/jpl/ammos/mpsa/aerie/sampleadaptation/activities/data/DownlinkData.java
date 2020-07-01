package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.ConsumableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates.totalDownlinkedDataBits;

/**
 * DownlinkData data from each channel according to downlinkPriority
 * as specified in Config until totalAmount has been reached
 *
 * @subsystem Data
 */
@ActivityType(name="DownlinkData", generateMapper=true)
public class DownlinkData implements Activity {

    @Parameter
    public boolean downlinkAll = true;

    @Parameter
    public int totalBits = 0;

    @Override
    public List<String> validateParameters() {
        final List<String> failures = new ArrayList<>();

        if (this.totalBits <= 0  && !downlinkAll) {
            failures.add("downlinked amount must be positive");
        }

        return failures;
    }

    @Override
    public void modelEffects() {
        if (downlinkAll) {
            downlinkAll();
        } else {
            downlinkBits(totalBits);
        }
    }

    /**
     * Downlink all data from all channels in order by priority
     */
    private void downlinkAll() {
        for (final var channel : Config.downlinkPriority) {
            // TODO: Remove cast when data channel states become integers
            int downlinkVolume = (int)channel.get();
            downlinkChannelVolume(channel, downlinkVolume);
        }
    }

    /**
     * Downlink a total volume across all channels in order by priority
     * Once the total volume has been reached, now more downlink occurs
     * @param totalBits - The total amount of bits to downlink
     */
    private void downlinkBits(int totalBits) {
        int remainingBits = totalBits;
        for (final var channel : Config.downlinkPriority) {
            // TODO: Remove cast when data channel states become integers
            int downlinkVolume = Math.min((int)channel.get(), remainingBits);
            downlinkChannelVolume(channel, downlinkVolume);

            remainingBits -= downlinkVolume;
            if (remainingBits == 0) break;
        }
    }

    private void downlinkChannelVolume(ConsumableState channel, int downlinkVolume) {
        channel.add(-downlinkVolume);
        totalDownlinkedDataBits.add(+downlinkVolume);

        // Wait for downlink duration
        long downlinkDuration = downlinkVolume / Config.downlinkRate;
        ctx.delay(Duration.of(downlinkDuration, TimeUnit.SECONDS));
    }
}
