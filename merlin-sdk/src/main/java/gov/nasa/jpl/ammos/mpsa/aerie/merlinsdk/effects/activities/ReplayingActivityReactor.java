package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import java.util.UUID;

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

  public void execute(final ReactionContext<T, Activity, Event> ctx, final String activityId, final Activity activity) {
    this.executor.execute(ctx, activityId, activity);
  }
}
