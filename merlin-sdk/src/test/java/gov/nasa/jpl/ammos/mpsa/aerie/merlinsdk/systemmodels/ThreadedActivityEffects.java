package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public final class ThreadedActivityEffects {
    private final BiConsumer<Duration, Runnable> scheduleEvent;

    private ThreadedActivityEffects(final BiConsumer<Duration, Runnable> scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    public static void enter(final BiConsumer<Duration, Runnable> scheduleEvent, final Runnable scope) {
        final var effects = new ThreadedActivityEffects(scheduleEvent);
        ActivityEffects.enter(
            effects.new Provider(null),
            () -> ActivityEffects.spawn(Duration.ZERO, scope));
    }

    private final class Provider implements ActivityEffects.Provider {
        private volatile boolean isActive = false;

        private final Provider parent;
        private final Set<Provider> uncompletedChildren = new HashSet<>();
        private volatile boolean isWaitingForChildren = false;

        public Provider(final Provider parent) {
            this.parent = parent;
        }

        private void start(final Runnable activity) {
            try {
                // Wait until this activity is allowed to continue.
                while (!this.isActive) {}

                activity.run();
                this.waitForChildren();

                // Tell the parent that its child has completed.
                if (this.parent != null) {
                    this.parent.uncompletedChildren.remove(this);
                    if (this.parent.uncompletedChildren.isEmpty() && this.parent.isWaitingForChildren) {
                        ThreadedActivityEffects.this.scheduleEvent.accept(Duration.ZERO, this.parent::resumeFromChildren);
                    }
                }
            } finally {
                // Yield control back to the coordinator.
                this.isActive = false;
            }
        }

        private void yield() {
            // Yield control back to the coordinator.
            this.isActive = false;
            // Wait until this activity is allowed to continue.
            while (!this.isActive) {}
        }

        private void resume() {
            // Resume the thread.
            this.isActive = true;
            // Wait until the thread has yielded.
            while (this.isActive) {}
        }

        private void resumeFromChildren() {
            this.isWaitingForChildren = false;

            this.resume();
        }

        @Override
        public void delay(final Duration duration) {
            ThreadedActivityEffects.this.scheduleEvent.accept(duration, this::resume);

            this.yield();
        }

        @Override
        public void spawn(final Duration duration, final Runnable activity) {
            final var child = new Provider(this);
            this.uncompletedChildren.add(child);

            ThreadedActivityEffects.this.scheduleEvent.accept(duration, () -> {
                final var t = new Thread(() -> ActivityEffects.enter(child, () -> child.start(activity)));
                t.setDaemon(true);
                t.start();

                child.resume();
            });
        }

        @Override
        public void waitForChildren() {
            if (!this.uncompletedChildren.isEmpty()) {
                this.isWaitingForChildren = true;

                this.yield();
            }
        }
    }
}
