package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public final class RestartingActivityEffects {
    private final Queue<Task> queue = new PriorityQueue<>(Comparator.comparing(t -> t.resumeTime));

    private Instant currentTime;
    private Task currentTask = null;

    private RestartingActivityEffects(final Instant initialInstant) {
        this.currentTime = initialInstant;
    }

    public static Instant execute(final Instant initialInstant, final Runnable scope) {
        final var effects = new RestartingActivityEffects(initialInstant);
        final var provider = effects.new Provider();

        effects.queue.add(new Task(effects.currentTime, effects.currentTime, scope, null));

        ActivityEffects.enter(provider, () -> {
            while (!effects.queue.isEmpty()) {
                final var task = effects.queue.remove();

                effects.currentTime = task.startTime;
                effects.currentTask = task;

                try {
                    task.scope.run();
                    provider.waitForChildren();
                    if (task.parent != null) {
                        task.parent.runningChildren.remove(task);
                        if (task.parent.waitingForChildren && task.parent.runningChildren.isEmpty()) {
                            task.parent.waitingForChildren = false;
                            task.parent.resumeTime = effects.currentTime;
                            task.parent.childrenResumptions.add(effects.currentTime);
                            effects.queue.add(task.parent);
                        }
                    }
                } catch (final Rescheduled ignored) {
                }
            }
        });

        return effects.currentTime;
    }

    private static final class Task {
        public final Instant startTime;
        public final Runnable scope;
        public final Task parent;
        public final Set<Task> runningChildren = new HashSet<>();
        public final List<Instant> childrenResumptions = new ArrayList<>();
        public int nextChildrenResumption = 0;

        public Instant resumeTime;
        public boolean waitingForChildren = false;

        public Task(final Instant startTime, final Instant resumeTime, final Runnable scope, final Task parent) {
            this.startTime = startTime;
            this.resumeTime = resumeTime;
            this.scope = scope;
            this.parent = parent;
        }
    }

    private static class Rescheduled extends RuntimeException {}
    private static final Rescheduled Rescheduled = new Rescheduled();

    private final class Provider implements ActivityEffects.Provider {
        @Override
        public void delay(final Duration duration) {
            final var resumeTime = RestartingActivityEffects.this.currentTime.plus(duration);

            if (this.replaying()) {
                // Replaying; skip effects.
                RestartingActivityEffects.this.currentTime = resumeTime;
                return;
            }

            final var task = RestartingActivityEffects.this.currentTask;
            RestartingActivityEffects.this.queue.add(task);

            task.resumeTime = resumeTime;
            task.nextChildrenResumption = 0;
            throw Rescheduled;
        }

        @Override
        public void spawn(final Duration duration, final Runnable activity) {
            if (this.replaying()) return;

            final var task = RestartingActivityEffects.this.currentTask;
            final var currentTime = RestartingActivityEffects.this.currentTime;

            final var resumeTime = currentTime.plus(duration);
            final var newTask = new Task(resumeTime, resumeTime, activity, task);

            task.runningChildren.add(newTask);
            RestartingActivityEffects.this.queue.add(newTask);
        }

        @Override
        public void waitForChildren() {
            final var task = RestartingActivityEffects.this.currentTask;

            if (this.replaying()) {
                RestartingActivityEffects.this.currentTime = task.childrenResumptions.get(task.nextChildrenResumption);
                task.nextChildrenResumption += 1;
                return;
            }

            if (task.runningChildren.isEmpty()) {
                task.childrenResumptions.add(RestartingActivityEffects.this.currentTime);
                return;
            }

            task.waitingForChildren = true;
            task.nextChildrenResumption = 0;
            throw Rescheduled;
        }

        @Override
        public Instant now() {
            return RestartingActivityEffects.this.currentTime;
        }

        @Override
        public boolean replaying() {
            final var task = RestartingActivityEffects.this.currentTask;
            final var currentTime = RestartingActivityEffects.this.currentTime;

            return task.resumeTime.isAfter(currentTime);
        }
    }
}
