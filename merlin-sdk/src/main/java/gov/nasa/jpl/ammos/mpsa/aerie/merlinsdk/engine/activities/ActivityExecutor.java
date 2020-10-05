package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities;

public interface ActivityExecutor<T, Event, Activity> {
  void execute(ReactionContext<T, Event, Activity> ctx, String activityId, Activity activity);
}
