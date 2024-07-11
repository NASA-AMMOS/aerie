package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;

import java.time.Instant;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

public class TimeTaggingLogger implements SimpleLogger {
    private final Instant planStart;
    private final SimpleLogger logger;

    public TimeTaggingLogger(Instant planStart, SimpleLogger logger) {
        this.planStart = planStart;
        this.logger = logger;
    }

    @Override
    public void log(String messageFormat, Object... args) {
        Instant time = planStart.plusNanos(Resources.currentTime().in(MICROSECONDS) * 1_000);
        logger.log("%s - %s".formatted(time, messageFormat), args);
    }
}
