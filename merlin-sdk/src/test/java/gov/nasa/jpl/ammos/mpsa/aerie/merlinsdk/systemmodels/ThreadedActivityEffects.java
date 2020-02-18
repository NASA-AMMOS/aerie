package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public final class ThreadedActivityEffects {
    private final Queue<Pair<Instant, Runnable>> queue = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));

    private ThreadedActivityEffects() {}

    public static Instant execute(final Instant initialInstant, final Runnable scope) {
        final var effects = new ThreadedActivityEffects();
        final var provider = effects.new Provider(initialInstant, null);

        final var t = new Thread(() -> ActivityEffects.enter(provider, () -> provider.start(scope)));
        t.setDaemon(true);
        t.start();

        effects.queue.add(Pair.of(initialInstant, () -> provider.resumeFromChildren(initialInstant)));

        var endTime = initialInstant;
        while (!effects.queue.isEmpty()) {
            final var job = effects.queue.remove();
            endTime = job.getLeft();
            job.getRight().run();
        }

        return endTime;
    }

    private final class Provider implements ActivityEffects.Provider {
        private volatile boolean isActive = false;

        private Instant now;
        private final Provider parent;
        private final Set<Provider> uncompletedChildren = new HashSet<>();
        private volatile boolean isWaitingForChildren = false;

        public Provider(final Instant startTime, final Provider parent) {
            this.now = startTime;
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
                        ThreadedActivityEffects.this.queue.add(Pair.of(this.now, () -> this.parent.resumeFromChildren(this.now)));
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

        private void resume(final Instant resumeTime) {
            this.now = resumeTime;

            // Resume the thread.
            this.isActive = true;
            // Wait until the thread has yielded.
            while (this.isActive) {}
        }

        private void resumeFromChildren(final Instant resumeTime) {
            this.isWaitingForChildren = false;

            this.resume(resumeTime);
        }

        @Override
        public void delay(final Duration duration) {
            if (duration.isNegative()) throw new RuntimeException("Cannot wait for a negative duration");

            ThreadedActivityEffects.this.queue.add(Pair.of(this.now.plus(duration), () -> this.resume(this.now.plus(duration))));

            this.yield();
        }

        @Override
        public void spawn(final Duration duration, final Runnable activity) {
            if (duration.isNegative()) throw new RuntimeException("Cannot wait for a negative duration");

            final var child = new Provider(this.now, this);
            this.uncompletedChildren.add(child);

            ThreadedActivityEffects.this.queue.add(Pair.of(this.now.plus(duration), () -> {
                final var t = new Thread(() -> ActivityEffects.enter(child, () -> child.start(activity)));
                t.setDaemon(true);
                t.start();

                child.resume(this.now.plus(duration));
            }));
        }

        @Override
        public void waitForChildren() {
            if (!this.uncompletedChildren.isEmpty()) {
                this.isWaitingForChildren = true;

                this.yield();
            }
        }

        @Override
        public Instant now() {
            return this.now;
        }
    }
}
