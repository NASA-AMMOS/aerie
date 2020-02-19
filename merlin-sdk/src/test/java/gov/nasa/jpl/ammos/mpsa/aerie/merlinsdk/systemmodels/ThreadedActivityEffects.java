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
    private Instant currentTime;

    private ThreadedActivityEffects(final Instant initialInstant) {
        this.currentTime = initialInstant;
    }

    public static Instant execute(final Instant initialInstant, final Runnable scope) {
        final var effects = new ThreadedActivityEffects(initialInstant);
        final var root = effects.new Provider();

        effects.queue.add(Pair.of(initialInstant, () -> {
            final var t = new Thread(() -> ActivityEffects.enter(root, () -> root.start(scope)));
            t.setDaemon(true);
            t.start();

            root.resume();
        }));

        while (!effects.queue.isEmpty()) {
            final var job = effects.queue.remove();
            effects.currentTime = job.getLeft();
            job.getRight().run();
        }

        return effects.currentTime;
    }

    private final class Provider implements ActivityEffects.Provider {
        private volatile boolean isActive = false;

        private final Provider parent;
        private final Set<Provider> uncompletedChildren = new HashSet<>();
        private volatile boolean isWaitingForChildren = false;

        public Provider(final Provider parent) {
            this.parent = parent;
        }

        public Provider() {
            this(null);
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
                        ThreadedActivityEffects.this.queue.add(Pair.of(ThreadedActivityEffects.this.currentTime, () -> this.parent.resumeFromChildren()));
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
            if (duration.isNegative()) throw new RuntimeException("Cannot wait for a negative duration");

            ThreadedActivityEffects.this.queue.add(Pair.of(ThreadedActivityEffects.this.currentTime.plus(duration), this::resume));

            this.yield();
        }

        @Override
        public void spawn(final Duration duration, final Runnable activity) {
            if (duration.isNegative()) throw new RuntimeException("Cannot wait for a negative duration");

            final var child = new Provider(this);
            this.uncompletedChildren.add(child);

            ThreadedActivityEffects.this.queue.add(Pair.of(ThreadedActivityEffects.this.currentTime.plus(duration), () -> {
                final var t = new Thread(() -> ActivityEffects.enter(child, () -> child.start(activity)));
                t.setDaemon(true);
                t.start();

                child.resume();
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
            return ThreadedActivityEffects.this.currentTime;
        }
    }
}
