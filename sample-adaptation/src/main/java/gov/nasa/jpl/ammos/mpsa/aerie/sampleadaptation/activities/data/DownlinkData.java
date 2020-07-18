package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.DoubleState;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.Config;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.SECONDS;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration.duration;
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
            final var downlinkVolume = channel.get();
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
            // TODO: Remove coercion when data channel states become integers
            int downlinkVolume = Math.min(channel.get().intValue(), remainingBits);
            downlinkChannelVolume(channel, downlinkVolume);

            remainingBits -= downlinkVolume;
            if (remainingBits == 0) break;
        }
    }

    // TODO: Change from `double` to `int` when data channel states become integers
    private void downlinkChannelVolume(DoubleState channel, double downlinkVolume) {
        channel.add(-downlinkVolume);
        totalDownlinkedDataBits.add(+downlinkVolume);

        // Wait for downlink duration
        long downlinkDuration = ((long) downlinkVolume) / Config.downlinkRate;
        ctx.delay(duration(downlinkDuration, SECONDS));
    }
}
