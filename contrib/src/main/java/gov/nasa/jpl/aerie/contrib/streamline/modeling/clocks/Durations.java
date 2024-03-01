package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// REVIEW - Should these be moved into Duration itself?
/**
 * Utilities for {@link Duration}
 */
public final class Durations {
    private Durations() {}

    public static Duration between(Instant start, Instant end) {
        return Duration.of(
                ChronoUnit.MICROS.between(start, end),
                Duration.MICROSECONDS);
    }
}
