package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

public interface ActivityExecutor<T, Activity, Event> {
  void execute(ReactionContext<T, Activity, Event> ctx, String activityId, Activity activity);
}
