package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class ThreadedActivityEffects {
    // TODO: Accept a thread pool on construction, instead of sharing a single static pool.
    private static final ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        // Daemonize all threads in this pool, so that they don't block the application on shutdown.
        // TODO: Properly call `shutdown` on the pool instead, once the pool is provided at construction time.
        final var t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    private final Queue<Pair<Instant, Provider>> queue = new PriorityQueue<>(Comparator.comparing(Pair::getLeft));
    private Instant currentTime;

    private ThreadedActivityEffects(final Instant initialInstant) {
        this.currentTime = initialInstant;
    }

    public static Instant execute(final Instant initialInstant, final Runnable scope) {
        final var effects = new ThreadedActivityEffects(initialInstant);

        {
            final var root = effects.new Provider();
            effects.spawnInThread(root, scope);
            effects.resumeAfter(Duration.ZERO, root);
        }

        while (!effects.queue.isEmpty()) {
            final var job = effects.queue.remove();
            final var jobTime = job.getKey();
            final var provider = job.getValue();

            effects.currentTime = jobTime;

            // Resume the thread.
            provider.isActive = true;
            // Wait until the thread has yielded.
            while (provider.isActive) {}
        }

        return effects.currentTime;
    }

    private void spawnInThread(final Provider provider, final Runnable scope) {
        threadPool.execute(() -> provider.start(scope));
    }

    private void resumeAfter(final Duration duration, final Provider provider) {
        this.queue.add(Pair.of(this.currentTime.plus(duration), provider));
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

                ActivityEffects.enter(this, activity);
                this.waitForChildren();

                // Tell the parent that its child has completed.
                if (this.parent != null) {
                    this.parent.uncompletedChildren.remove(this);
                    if (this.parent.uncompletedChildren.isEmpty() && this.parent.isWaitingForChildren) {
                        this.parent.isWaitingForChildren = false;
                        ThreadedActivityEffects.this.resumeAfter(Duration.ZERO, this.parent);
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

        @Override
        public void delay(final Duration duration) {
            if (duration.isNegative()) throw new RuntimeException("Cannot wait for a negative duration");

            ThreadedActivityEffects.this.resumeAfter(duration, this);

            this.yield();
        }

        @Override
        public void spawn(final Duration duration, final Runnable scope) {
            if (duration.isNegative()) throw new RuntimeException("Cannot wait for a negative duration");

            final var child = new Provider(this);
            ThreadedActivityEffects.this.spawnInThread(child, scope);
            ThreadedActivityEffects.this.resumeAfter(duration, child);

            this.uncompletedChildren.add(child);
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

        @Override
        public boolean replaying() {
            return false;
        }
    }
}
