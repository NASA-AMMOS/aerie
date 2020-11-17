package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SolvableDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ReplayingTask<$Schema, $Timeline extends $Schema, Event, TaskSpec>
    implements Task<$Timeline, Event, TaskSpec>
{
  private final ProxyContext<$Schema, Event, TaskSpec> rootContext;
  private final Runnable task;

  private Optional<History<$Timeline, Event>> initialTime = Optional.empty();
  private final List<ActivityBreadcrumb<$Timeline, Event>> breadcrumbs = new ArrayList<>();

  public ReplayingTask(final ProxyContext<$Schema, Event, TaskSpec> rootContext, final Runnable task) {
    this.rootContext = rootContext;
    this.task = task;
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline, Event, TaskSpec> scheduler) {
    final var context = this.new ReplayingReactionContext(this.initialTime, scheduler);

    if (this.initialTime.isEmpty()) {
      this.initialTime = Optional.of(scheduler.now());
    }

    {
      final var oldContext = this.rootContext.getTarget();
      this.rootContext.setTarget(context);

      try {
        this.task.run();
        // If we get here, the activity has completed normally.
      } catch (final Yield ignored) {
        // If we get here, the activity has suspended.
      } finally {
        this.rootContext.setTarget(oldContext);
      }
    }

    return context.getStatus();
  }

  // Since this exception is just used to transfer control out of an activity,
  //   we can pre-allocate a single instance as a unique token
  //   to avoid some of the overhead of exceptions
  //   (most notably the call stack snapshotting).
  private static final class Yield extends RuntimeException {}
  private static final Yield Yield = new Yield();

  private final class ReplayingReactionContext implements Context<$Schema, Event, TaskSpec> {
    private final Scheduler<$Timeline, Event, TaskSpec> scheduler;
    private TaskStatus<$Timeline> status = TaskStatus.completed();

    private Optional<History<$Timeline, Event>> history;
    private int nextBreadcrumbIndex = 0;

    public ReplayingReactionContext(
        final Optional<History<$Timeline, Event>> initialTime,
        final Scheduler<$Timeline, Event, TaskSpec> scheduler)
    {
      this.history = initialTime;
      this.scheduler = scheduler;
    }

    private TaskStatus<$Timeline> getStatus() {
      return this.status;
    }

    @Override
    public History<$Timeline, Event> now() {
      return this.history.orElseGet(this.scheduler::now);
    }

    @Override
    public void emit(final Event event) {
      if (this.history.isEmpty()) {
        this.scheduler.emit(event);
      } else {
        // TODO: Avoid leaving garbage behind -- find some way to remove regenerated events
        //   on dead-end branches when references to it disappear.
        this.history = this.history.map(now -> now.emit(event));
      }
    }

    @Override
    public double ask(final RealResource<? super History<? extends $Schema, ?>> resource) {
      final var dynamics = SolvableDynamics.real(resource.getDynamics(now()).getDynamics());

      return this.scheduler.ask(dynamics, Duration.ZERO);
    }

    @Override
    public <T> T ask(final DiscreteResource<? super History<? extends $Schema, ?>, T> resource) {
      final var dynamics = SolvableDynamics.discrete(resource.getDynamics(now()).getDynamics());

      return this.scheduler.ask(dynamics, Duration.ZERO);
    }

    @Override
    public String spawn(final TaskSpec taskSpec) {
      if (this.history.isEmpty()) {
        // We're running normally.
        final var id = this.scheduler.spawn(taskSpec);

        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
        this.nextBreadcrumbIndex += 1;

        return id;
      } else {
        return respawn();
      }
    }

    @Override
    public String defer(final Duration duration, final TaskSpec taskSpec) {
      if (this.history.isEmpty()) {
        // We're running normally.
        final var id = this.scheduler.defer(duration, taskSpec);

        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
        this.nextBreadcrumbIndex += 1;

        return id;
      } else {
        return respawn();
      }
    }

    @Override
    public void delay(final Duration duration) {
      if (this.history.isEmpty()) {
        // We're running normally.
        this.status = TaskStatus.delayed(duration);
        throw Yield;
      } else {
        readvance();
      }
    }

    @Override
    public void waitFor(final String id) {
      if (this.history.isEmpty()) {
        // We're running normally.
        this.status = TaskStatus.awaiting(id);
        throw Yield;
      } else {
        readvance();
      }
    }

    @Override
    public void waitFor(final RealResource<? super History<? extends $Schema, ?>> resource, final RealCondition condition) {
      if (this.history.isEmpty()) {
        // We're running normally.
        this.status = TaskStatus.awaiting(
            (now) -> resource.getDynamics(now).<SolvableDynamics<Double, RealCondition>>map(SolvableDynamics::real),
            condition);
        throw Yield;
      } else {
        readvance();
      }
    }

    @Override
    public <T> void waitFor(final DiscreteResource<? super History<? extends $Schema, ?>, T> resource, final Set<T> condition) {
      if (this.history.isEmpty()) {
        // We're running normally.
        this.status = TaskStatus.awaiting(
            (now) -> resource.getDynamics(now).map(SolvableDynamics::discrete),
            condition);
        throw Yield;
      } else {
        readvance();
      }
    }

    private void readvance() {
      if (this.nextBreadcrumbIndex >= ReplayingTask.this.breadcrumbs.size()) {
        // We've just now caught up.
        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(this.scheduler.now()));
        this.nextBreadcrumbIndex += 1;

        this.history = Optional.empty();
      } else {
        // We're still behind -- jump to the next breadcrumb.
        final var breadcrumb = ReplayingTask.this.breadcrumbs.get(this.nextBreadcrumbIndex);
        this.nextBreadcrumbIndex += 1;

        if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
          throw new Error("Expected Advance breadcrumb; got " + breadcrumb.getClass().getName());
        }

        this.history = Optional.of(((ActivityBreadcrumb.Advance<$Timeline, Event>) breadcrumb).next);
      }
    }

    private String respawn() {
      if (this.nextBreadcrumbIndex >= ReplayingTask.this.breadcrumbs.size()) {
        // We've just now caught up.
        throw new Error("Expected a Spawn breadcrumb while replaying; found none.");
      } else {
        // We're still behind -- jump to the next breadcrumb.
        final var breadcrumb = ReplayingTask.this.breadcrumbs.get(this.nextBreadcrumbIndex);
        this.nextBreadcrumbIndex += 1;

        if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
          throw new Error("Expected Spawn breadcrumb; got " + breadcrumb.getClass().getName());
        }

        return ((ActivityBreadcrumb.Spawn<$Timeline, Event>) breadcrumb).activityId;
      }
    }
  }
}
