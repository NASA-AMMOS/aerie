package gov.nasa.jpl.aerie.contrib.streamline.debugging;

import gov.nasa.jpl.aerie.merlin.framework.Registrar;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class Logger {
    private static final int LEVEL_INDICATOR_SIZE = Arrays.stream(LogLevel.values())
            .map(v -> v.toString().length())
            .max(Integer::compareTo)
            .orElseThrow();
    private static final String LOG_MESSAGE_FORMAT = "%-" + LEVEL_INDICATOR_SIZE + "s - %s";

    private final Map<LogLevel, TimeTaggingLogger> subLoggers;

    public Logger(Registrar registrar, Instant planStart) {
        // TODO - refactor the plan start out, by instead building a global sim clock to do the same job.
        //  To do that "right", the global clock should be a resource with custom dynamics for continuous Instant clocks.
        subLoggers = Arrays.stream(LogLevel.values()).collect(toMap(
                level -> level,
                level -> new TimeTaggingLogger(planStart, new TopicLogger(level.name(), registrar))));
    }

    public void log(LogLevel level, String messageFormat, Object... args) {
        String message = messageFormat.formatted(args);
        subLoggers.get(level).log(LOG_MESSAGE_FORMAT.formatted(level, message));
    }

    public void debug(String messageFormat, Object... args) {
        log(LogLevel.DEBUG, messageFormat, args);
    }

    public void info(String messageFormat, Object... args) {
        log(LogLevel.INFO, messageFormat, args);
    }

    public void warning(String messageFormat, Object... args) {
        log(LogLevel.WARNING, messageFormat, args);
    }

    public void error(String messageFormat, Object... args) {
        log(LogLevel.ERROR, messageFormat, args);
    }

    public enum LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
}
