package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.TaskFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.TaskScheduler;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Objects;

public final class ReplayingTask<T, Event, Activity> implements SimulationTask<T, Event> {
  private final TaskFactory<T, Event, Activity> factory;
  private final String activityId;
  private final Activity activity;
  private final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs;

  public ReplayingTask(
      final TaskFactory<T, Event, Activity> factory,
      final String activityId,
      final Activity activity,
      final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs)
  {
    this.factory = Objects.requireNonNull(factory);
    this.activityId = Objects.requireNonNull(activityId);
    this.activity = Objects.requireNonNull(activity);
    this.breadcrumbs = Objects.requireNonNull(breadcrumbs);
  }

  public ReplayingTask(
      final TaskFactory<T, Event, Activity> factory,
      final String activityId,
      final Activity activity)
  {
    this(factory, activityId, activity, TreePVector.empty());
  }

  public ReplayingTask<T, Event, Activity> spawned(final String childId) {
    final var breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Spawn<>(childId));
    return new ReplayingTask<>(this.factory, this.activityId, this.activity, breadcrumbs);
  }

  public ReplayingTask<T, Event, Activity> advancedTo(final History<T, Event> timePoint) {
    final var breadcrumbs = this.breadcrumbs.plus(new ActivityBreadcrumb.Advance<>(timePoint));
    return new ReplayingTask<>(this.factory, this.activityId, this.activity, breadcrumbs);
  }

  public ActivityBreadcrumb<T, Event> getBreadcrumb(int index) {
    return this.breadcrumbs.get(index);
  }

  public int getBreadcrumbCount() {
    return this.breadcrumbs.size();
  }

  @Override
  public String getId() {
    return this.activityId;
  }

  @Override
  public History<T, Event> runFrom(final History<T, Event> history, final TaskScheduler<T, Event> scheduler) {
    final var context = new ReplayingReactionContext<>(this.factory, scheduler, this.advancedTo(history));

    // TODO: avoid using exceptions for control flow by wrapping the executor in a Thread
    try {
      this.factory.execute(context, this.activityId, this.activity);
      scheduler.complete();
    } catch (final ReplayingReactionContext.Defer request) {
      scheduler.defer(request.duration, context.getContinuation());
    } catch (final ReplayingReactionContext.Await request) {
      scheduler.await(request.activityId, context.getContinuation());
    }

    return context.now();
  }

  @Override
  public String toString() {
    return String.format("activity(step = %d, activity = %s", this.breadcrumbs.size(), this.activity);
  }
}
