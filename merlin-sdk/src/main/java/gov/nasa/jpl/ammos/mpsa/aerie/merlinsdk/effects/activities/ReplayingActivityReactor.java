package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;

import java.util.UUID;
import java.util.function.Consumer;

public final class ReplayingActivityReactor<T, Event, Activity> {
  private final ActivityExecutor<T, Activity, Event> executor;

  public ReplayingActivityReactor(final ActivityExecutor<T, Activity, Event> executor) {
    this.executor = executor;
  }

  public ActivityContinuation<T, Event, Activity> createSimulationTask(final Activity activity) {
    return new ActivityContinuation<>(this, UUID.randomUUID().toString(), activity);
  }

  public ActivityContinuation<T, Event, Activity> createSimulationTask(final String id, final Activity activity) {
    return new ActivityContinuation<>(this, id, activity);
  }

  public TaskFrame<T, Event> react(
      final History<T, Event> history,
      final Consumer<ScheduleItem<T, Event>> scheduler,
      final ActivityContinuation<T, Event, Activity> task)
  {
    final var context = new ReplayingReactionContext<>(this, scheduler, task.advancedTo(history).breadcrumbs);

    // TODO: avoid using exceptions for control flow by wrapping the executor in a Thread
    try {
      this.executor.execute(context, task.activityId, task.activity);

      scheduler.accept(new ScheduleItem.Complete<>(task.activityId));
    } catch (final ReplayingReactionContext.Defer request) {
      scheduler.accept(new ScheduleItem.Defer<>(
          request.duration,
          new ActivityContinuation<>(this, task.activityId, task.activity, context.getBreadcrumbs())));
    } catch (final ReplayingReactionContext.Await request) {
      scheduler.accept(new ScheduleItem.OnCompletion<>(
          request.activityId,
          new ActivityContinuation<>(this, task.activityId, task.activity, context.getBreadcrumbs())));
    }

    return new TaskFrame<>(context.getCurrentHistory(), context.getSpawns());
  }
}
