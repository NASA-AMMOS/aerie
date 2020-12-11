package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealCondition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ReplayingTask<$Schema, $Timeline extends $Schema>
    implements Task<$Timeline>
{
  private final ProxyContext<$Schema> rootContext;
  private final Runnable task;

  private Optional<History<$Timeline>> initialTime = Optional.empty();
  private final List<ActivityBreadcrumb<$Timeline>> breadcrumbs = new ArrayList<>();

  public ReplayingTask(final ProxyContext<$Schema> rootContext, final Runnable task) {
    this.rootContext = rootContext;
    this.task = task;
  }

  @Override
  public TaskStatus<$Timeline> step(final Scheduler<$Timeline> scheduler) {
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

  private final class ReplayingReactionContext implements Context<$Schema> {
    private final Scheduler<$Timeline> scheduler;
    private TaskStatus<$Timeline> status = TaskStatus.completed();

    private Optional<History<$Timeline>> history;
    private int nextBreadcrumbIndex = 0;

    public ReplayingReactionContext(
        final Optional<History<$Timeline>> initialTime,
        final Scheduler<$Timeline> scheduler)
    {
      this.history = initialTime;
      this.scheduler = scheduler;
    }

    private TaskStatus<$Timeline> getStatus() {
      return this.status;
    }

    @Override
    public History<$Timeline> now() {
      return this.history.orElseGet(this.scheduler::now);
    }

    @Override
    public <Event> void emit(final Event event, final Query<? super $Schema, Event, ?> query) {
      if (this.history.isEmpty()) {
        this.scheduler.emit(event, query);
      } else {
        // TODO: Avoid leaving garbage behind -- find some way to remove regenerated events
        //   on dead-end branches when references to it disappear.
        this.history = this.history.map(now -> now.emit(event, query));
      }
    }

    @Override
    public double ask(final RealResource<? super History<? extends $Schema>> resource) {
      return new RealSolver().valueAt(resource.getDynamics(now()).getDynamics(), Duration.ZERO);
    }

    @Override
    public <T> T ask(final DiscreteResource<? super History<? extends $Schema>, T> resource) {
      return new DiscreteSolver<T>().valueAt(resource.getDynamics(now()).getDynamics(), Duration.ZERO);
    }

    @Override
    public <Spec> String spawn(final Spec spec, final TaskSpecType<? super $Schema, Spec> type) {
      if (this.history.isEmpty()) {
        // We're running normally.
        final var id = this.scheduler.spawn(spec, type);

        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
        this.nextBreadcrumbIndex += 1;

        return id;
      } else {
        return respawn();
      }
    }

    @Override
    public <Spec> String defer(final Duration duration, final Spec spec, final TaskSpecType<? super $Schema, Spec> type) {
      if (this.history.isEmpty()) {
        // We're running normally.
        final var id = this.scheduler.defer(duration, spec, type);

        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
        this.nextBreadcrumbIndex += 1;

        return id;
      } else {
        return respawn();
      }
    }

    @Override
    public String spawn(final String type, final Map<String, SerializedValue> arguments) {
      if (this.history.isEmpty()) {
        // We're running normally.
        final var id = this.scheduler.spawn(type, arguments);

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
    public void waitFor(final RealResource<? super History<? extends $Schema>> resource, final RealCondition condition) {
      if (this.history.isEmpty()) {
        // We're running normally.
        this.status = TaskStatus.<$Timeline, RealDynamics, RealCondition>awaiting(
            new RealSolver(), resource::getDynamics, condition);
        throw Yield;
      } else {
        readvance();
      }
    }

    @Override
    public <T> void waitFor(final DiscreteResource<? super History<? extends $Schema>, T> resource, final Set<T> condition) {
      if (this.history.isEmpty()) {
        // We're running normally.
        this.status = TaskStatus.<$Timeline, T, Set<T>>awaiting(
            new DiscreteSolver<>(), resource::getDynamics, condition);
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

        this.history = Optional.of(((ActivityBreadcrumb.Advance<$Timeline>) breadcrumb).next);
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

        return ((ActivityBreadcrumb.Spawn<$Timeline>) breadcrumb).activityId;
      }
    }
  }
}
