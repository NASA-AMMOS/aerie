package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Objects;

public final class ActivityContinuation<T, Event, Activity> {
  public final Activity activity;
  public final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs;

  public ActivityContinuation(final Activity activity, final PVector<ActivityBreadcrumb<T, Event>> breadcrumbs) {
    this.activity = Objects.requireNonNull(activity);
    this.breadcrumbs = Objects.requireNonNull(breadcrumbs);
  }

  public ActivityContinuation(final Activity activity, final History<T, Event> startTime) {
    this(activity, TreePVector.singleton(new ActivityBreadcrumb.Advance<>(startTime)));
  }

  public ActivityContinuation(final Activity activity) {
    this(activity, TreePVector.empty());
  }

  public ActivityContinuation<T, Event, Activity> plus(final ActivityBreadcrumb<T, Event> breadcrumb) {
    return new ActivityContinuation<>(this.activity, this.breadcrumbs.plus(breadcrumb));
  }

  @Override
  public String toString() {
    return String.format("activity(step = %d, activity = %s", this.breadcrumbs.size(), this.activity);
  }
}
