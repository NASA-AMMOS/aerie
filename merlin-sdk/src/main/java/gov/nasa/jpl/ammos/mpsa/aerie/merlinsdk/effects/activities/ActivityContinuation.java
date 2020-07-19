package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Objects;
import java.util.function.Consumer;

public final class ActivityContinuation<T, Event, Activity> implements SimulationTask<T, Event> {
  private final ReplayingActivityReactor<T, Event, Activity> reactor;
  public final String activityId;
  public final Activity activity;
  public final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs;

  public ActivityContinuation(
      final ReplayingActivityReactor<T, Event, Activity> reactor,
      final String activityId,
      final Activity activity,
      final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs)
  {
    this.reactor = Objects.requireNonNull(reactor);
    this.activityId = Objects.requireNonNull(activityId);
    this.activity = Objects.requireNonNull(activity);
    this.breadcrumbs = Objects.requireNonNull(breadcrumbs);
  }

  public ActivityContinuation(
      final ReplayingActivityReactor<T, Event, Activity> reactor,
      final String activityId,
      final Activity activity)
  {
    this(reactor, activityId, activity, TreePVector.empty());
  }

  public ActivityContinuation<T, Event, Activity> spawned(final String childId) {
    final var breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Spawn<>(childId));
    return new ActivityContinuation<>(this.reactor, this.activityId, this.activity, breadcrumbs);
  }

  public ActivityContinuation<T, Event, Activity> advancedTo(final History<T, Event> timePoint) {
    final var breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Advance<>(timePoint));
    return new ActivityContinuation<>(this.reactor, this.activityId, this.activity, breadcrumbs);
  }

  @Override
  public String getId() {
    return this.activityId;
  }

  @Override
  public TaskFrame<T, Event> runFrom(
      final History<T, Event> history,
      final Consumer<ScheduleItem<T, Event>> scheduler)
  {
    final var context = new ReplayingReactionContext<>(this.reactor, scheduler, this.advancedTo(history).breadcrumbs);

    // TODO: avoid using exceptions for control flow by wrapping the executor in a Thread
    try {
      this.reactor.execute(context, this.activityId, this.activity);

      scheduler.accept(new ScheduleItem.Complete<>(this.activityId));
    } catch (final ReplayingReactionContext.Defer request) {
      scheduler.accept(new ScheduleItem.Defer<>(
          request.duration,
          new ActivityContinuation<>(this.reactor, this.activityId, this.activity, context.getBreadcrumbs())));
    } catch (final ReplayingReactionContext.Await request) {
      scheduler.accept(new ScheduleItem.OnCompletion<>(
          request.activityId,
          new ActivityContinuation<>(this.reactor, this.activityId, this.activity, context.getBreadcrumbs())));
    }

    return new TaskFrame<>(context.getCurrentHistory(), context.getSpawns());
  }

  @Override
  public String toString() {
    return String.format("activity(step = %d, activity = %s", this.breadcrumbs.size(), this.activity);
  }
}
