package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.util.Objects;

public final class ActivityEffects {
    private ActivityEffects() {}

    public interface Provider {
        void delay(final Duration duration);
        void spawn(final Duration duration, final Runnable activity);
        void waitForChildren();
        Instant now();
    }

    // It's best to think of a `ThreadLocal` not as data, but as a dynamically-scoped variable that exists somewhere
    // near the base of a thread's call stack. The actual `ThreadLocal` instance serves only to look up that data
    // in the ambient context of the active thread.
    private static final ThreadLocal<Provider> dynamicProvider = ThreadLocal.withInitial(() -> null);

    public static void enter(final Provider provider, final Runnable scope) {
        final var previous = dynamicProvider.get();

        dynamicProvider.set(provider);
        try {
            scope.run();
        } finally {
            dynamicProvider.set(previous);
        }
    }

    public static void delay(final Duration duration) {
        Objects
            .requireNonNull(dynamicProvider.get(), "delay cannot be called outside of activity context")
            .delay(duration);
    }

    public static void spawn(final Duration duration, final Runnable activity) {
        Objects
            .requireNonNull(dynamicProvider.get(), "spawn cannot be called outside of activity context")
            .spawn(duration, activity);
    }

    public static void waitForChildren() {
        Objects
            .requireNonNull(dynamicProvider.get(), "waitForChildren cannot be called outside of activity context")
            .waitForChildren();
    }

    public static Instant now() {
        return Objects
            .requireNonNull(dynamicProvider.get(), "now cannot be called outside of activity context")
            .now();
    }

    public static void delay(final long quantity, final TimeUnit units) {
        delay(Duration.fromQuantity(quantity, units));
    }

    public static void spawn(final long quantity, final TimeUnit units, final Runnable activity) {
        spawn(Duration.fromQuantity(quantity, units), activity);
    }

    public static void spawn(final Runnable activity) {
        spawn(Duration.ZERO, activity);
    }
}
