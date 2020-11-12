package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Scheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SolvableDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.ActivityBreadcrumb;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public final class ReplayingTask<$Timeline, Event, ActivityType, Resources>
    implements Task<$Timeline, Event, ActivityType>
{
  private final Resources resources;
  private final BiConsumer<Context<$Timeline, Event, ActivityType>, Resources> activity;

  private Optional<History<$Timeline, Event>> initialTime = Optional.empty();
  private final List<ActivityBreadcrumb<$Timeline, Event>> breadcrumbs = new ArrayList<>();

  public ReplayingTask(final Resources resources, final BiConsumer<Context<$Timeline, Event, ActivityType>, Resources> activity) {
    this.resources = resources;
    this.activity = activity;
  }

  public @Override
  ActivityStatus
  step(final Scheduler<$Timeline, Event, ActivityType> scheduler)
  {
    final var context = this.new ReplayingReactionContext(this.initialTime, scheduler);

    if (this.initialTime.isEmpty()) {
      this.initialTime = Optional.of(scheduler.now());
    }

    try {
      this.activity.accept(context, this.resources);
      return ActivityStatus.completed();
    } catch (final Defer request) {
      return ActivityStatus.delayed(request.duration);
    } catch (final Await request) {
      return ActivityStatus.awaiting(request.activityId);
    }
  }

  private final class ReplayingReactionContext implements Context<$Timeline, Event, ActivityType> {
    private final Scheduler<$Timeline, Event, ActivityType> scheduler;

    private Optional<History<$Timeline, Event>> history;
    private int nextBreadcrumbIndex = 0;

    public ReplayingReactionContext(
        final Optional<History<$Timeline, Event>> initialTime,
        final Scheduler<$Timeline, Event, ActivityType> scheduler)
    {
      this.history = initialTime;
      this.scheduler = scheduler;
    }

    @Override
    public History<$Timeline, ?> now() {
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
    public double ask(final RealResource<? super History<$Timeline, ?>> resource) {
      final var dynamics = SolvableDynamics.real(resource.getDynamics(now()).getDynamics());

      return this.scheduler.ask(dynamics, Duration.ZERO);
    }

    @Override
    public <T> T ask(final DiscreteResource<? super History<$Timeline, ?>, T> resource) {
      final var dynamics = SolvableDynamics.discrete(resource.getDynamics(now()).getDynamics());

      return this.scheduler.ask(dynamics, Duration.ZERO);
    }

    @Override
    public String spawn(final ActivityType activity) {
      if (this.history.isEmpty()) {
        // We're running normally.
        final var id = this.scheduler.spawn(activity);

        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
        this.nextBreadcrumbIndex += 1;

        return id;
      } else if (this.nextBreadcrumbIndex >= ReplayingTask.this.breadcrumbs.size()) {
        // We've just now caught up.
        throw new Error("Expected a Spawn breadcrumb while replaying; found none.");
      } else {
        // We're still behind -- jump to the next breadcrumb.
        final var breadcrumb = ReplayingTask.this.breadcrumbs.get(this.nextBreadcrumbIndex);
        this.nextBreadcrumbIndex += 1;

        if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
          throw new Error("Unexpected breadcrumb on spawn(): " + breadcrumb.getClass().getName());
        }

        return ((ActivityBreadcrumb.Spawn<$Timeline, Event>) breadcrumb).activityId;
      }
    }

    @Override
    public String defer(final Duration duration, final ActivityType activity) {
      if (this.history.isEmpty()) {
        // We're running normally.
        final var id = this.scheduler.defer(duration, activity);

        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Spawn<>(id));
        this.nextBreadcrumbIndex += 1;

        return id;
      } else if (this.nextBreadcrumbIndex >= ReplayingTask.this.breadcrumbs.size()) {
        // We've just now caught up.
        throw new Error("Expected a Spawn breadcrumb while replaying; found none.");
      } else {
        // We're still behind -- jump to the next breadcrumb.
        final var breadcrumb = ReplayingTask.this.breadcrumbs.get(this.nextBreadcrumbIndex);
        this.nextBreadcrumbIndex += 1;

        if (!(breadcrumb instanceof ActivityBreadcrumb.Spawn)) {
          throw new Error("Unexpected breadcrumb on spawn(): " + breadcrumb.getClass().getName());
        }

        return ((ActivityBreadcrumb.Spawn<$Timeline, Event>) breadcrumb).activityId;
      }
    }

    @Override
    public void delay(final Duration duration) {
      if (this.history.isEmpty()) {
        // We're running normally.
        throw new Defer(duration);
      } else if (this.nextBreadcrumbIndex >= ReplayingTask.this.breadcrumbs.size()) {
        // We've just now caught up.
        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(this.scheduler.now()));
        this.nextBreadcrumbIndex += 1;

        this.history = Optional.empty();
      } else {
        // We're still behind -- jump to the next breadcrumb.
        final var breadcrumb = ReplayingTask.this.breadcrumbs.get(this.nextBreadcrumbIndex);
        this.nextBreadcrumbIndex += 1;

        if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
          throw new Error("Unexpected breadcrumb on delay(): " + breadcrumb.getClass().getName());
        }

        this.history = Optional.of(((ActivityBreadcrumb.Advance<$Timeline, Event>) breadcrumb).next);
      }
    }

    @Override
    public void waitFor(final String id) {
      if (this.history.isEmpty()) {
        // We're running normally.
        throw new Await(id);
      } else if (this.nextBreadcrumbIndex >= ReplayingTask.this.breadcrumbs.size()) {
        // We've just now caught up.
        ReplayingTask.this.breadcrumbs.add(new ActivityBreadcrumb.Advance<>(this.scheduler.now()));
        this.nextBreadcrumbIndex += 1;

        this.history = Optional.empty();
      } else {
        // We're still behind -- jump to the next breadcrumb.
        final var breadcrumb = ReplayingTask.this.breadcrumbs.get(this.nextBreadcrumbIndex);
        this.nextBreadcrumbIndex += 1;

        if (!(breadcrumb instanceof ActivityBreadcrumb.Advance)) {
          throw new Error("Unexpected breadcrumb on waitFor(): " + breadcrumb.getClass().getName());
        }

        this.history = Optional.of(((ActivityBreadcrumb.Advance<$Timeline, Event>) breadcrumb).next);
      }
    }
  }

  private static final class Defer extends RuntimeException {
    public final Duration duration;

    private Defer(final Duration duration) {
      this.duration = duration;
    }
  }

  private static final class Await extends RuntimeException {
    public final String activityId;

    private Await(final String activityId) {
      this.activityId = activityId;
    }
  }
}
