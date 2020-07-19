package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import java.util.UUID;

public final class TaskFactory<T, Event, Activity> {
  private final ActivityExecutor<T, Event, Activity> executor;

  public TaskFactory(final ActivityExecutor<T, Event, Activity> executor) {
    this.executor = executor;
  }

  public ReplayingTask<T, Event, Activity> createReplayingTask(final Activity activity) {
    return this.createReplayingTask(UUID.randomUUID().toString(), activity);
  }

  public ReplayingTask<T, Event, Activity> createReplayingTask(final String id, final Activity activity) {
    return new ReplayingTask<>(this, id, activity);
  }

  public void execute(final ReactionContext<T, Event, Activity> ctx, final String activityId, final Activity activity) {
    this.executor.execute(ctx, activityId, activity);
  }
}
