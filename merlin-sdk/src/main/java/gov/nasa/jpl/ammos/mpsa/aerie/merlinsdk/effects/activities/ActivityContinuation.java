package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Objects;

public final class ActivityContinuation<T, Event, Activity> implements SimulationTask {
  public final String activityId;
  public final Activity activity;
  public final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs;

  public ActivityContinuation(
      final String activityId,
      final Activity activity,
      final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs)
  {
    this.activityId = Objects.requireNonNull(activityId);
    this.activity = Objects.requireNonNull(activity);
    this.breadcrumbs = Objects.requireNonNull(breadcrumbs);
  }

  public ActivityContinuation(final String activityId, final Activity activity, final History<T, Event> startTime) {
    this(activityId, activity, TreePVector.singleton(new ActivityBreadcrumb.Advance<>(startTime)));
  }

  public ActivityContinuation(final String activityId, final Activity activity) {
    this(activityId, activity, TreePVector.empty());
  }

  public ActivityContinuation<T, Event, Activity> plus(final ActivityBreadcrumb<T, Event> breadcrumb) {
    return new ActivityContinuation<>(this.activityId, this.activity, this.breadcrumbs.plus(breadcrumb));
  }

  @Override
  public String getId() {
    return this.activityId;
  }

  @Override
  public String toString() {
    return String.format("activity(step = %d, activity = %s", this.breadcrumbs.size(), this.activity);
  }
}
