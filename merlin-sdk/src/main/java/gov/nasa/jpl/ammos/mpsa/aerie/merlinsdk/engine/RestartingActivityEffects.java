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

public final class RestartingActivityEffects implements ActivityEffects.Provider {
    private final Queue<Task> queue = new PriorityQueue<>(Comparator.comparing(t -> t.resumeTime));

    private Instant currentTime;
    private Task currentTask = null;

    private RestartingActivityEffects(final Instant initialInstant, final Runnable rootScope) {
        this.currentTime = initialInstant;
        this.queue.add(new Task(initialInstant, initialInstant, rootScope, null));
    }

    public static Instant execute(final Instant initialInstant, final Runnable scope) {
        final var effects = new RestartingActivityEffects(initialInstant, scope);
        ActivityEffects.enter(effects, effects::run);
        return effects.currentTime;
    }

    private void run() {
        while (!this.queue.isEmpty()) {
            final var task = this.queue.remove();

            this.currentTime = task.startTime;
            this.currentTask = task;

            try {
                task.scope.run();
                this.waitForChildren();

                if (task.parent != null) {
                    task.parent.runningChildren.remove(task);
                    if (task.parent.waitingForChildren && task.parent.runningChildren.isEmpty()) {
                        task.parent.waitingForChildren = false;
                        task.parent.resumeTime = this.currentTime;
                        task.parent.childrenResumptions.add(this.currentTime);
                        this.queue.add(task.parent);
                    }
                }
            } catch (final Rescheduled ignored) {
            }
        }
    }

    @Override
    public void delay(final Duration duration) {
        final var resumeTime = this.currentTime.plus(duration);

        if (this.replaying()) {
            // Replaying; skip effects.
            this.currentTime = resumeTime;
            return;
        }

        final var task = this.currentTask;
        this.queue.add(task);

        task.resumeTime = resumeTime;
        task.nextChildrenResumption = 0;
        throw Rescheduled;
    }

    @Override
    public void spawn(final Duration duration, final Runnable activity) {
        if (this.replaying()) return;

        final var resumeTime = this.currentTime.plus(duration);
        final var newTask = new Task(resumeTime, resumeTime, activity, this.currentTask);

        this.currentTask.runningChildren.add(newTask);
        this.queue.add(newTask);
    }

    @Override
    public void waitForChildren() {
        if (this.replaying()) {
            this.currentTime = this.currentTask.childrenResumptions.get(this.currentTask.nextChildrenResumption);
            this.currentTask.nextChildrenResumption += 1;
            return;
        }

        if (this.currentTask.runningChildren.isEmpty()) {
            this.currentTask.childrenResumptions.add(this.currentTime);
            return;
        }

        this.currentTask.waitingForChildren = true;
        this.currentTask.nextChildrenResumption = 0;
        throw Rescheduled;
    }

    @Override
    public Instant now() {
        return this.currentTime;
    }

    @Override
    public boolean replaying() {
        return this.currentTask.resumeTime.isAfter(this.currentTime);
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
}
