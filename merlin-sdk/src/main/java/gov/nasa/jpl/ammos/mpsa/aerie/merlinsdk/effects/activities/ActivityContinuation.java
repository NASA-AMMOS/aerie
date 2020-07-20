package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Map;
import java.util.Objects;

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
      final Activity activity,
      final History<T, Event> startTime)
  {
    this(reactor, activityId, activity, TreePVector.singleton(new ActivityBreadcrumb.Advance<>(startTime)));
  }

  public ActivityContinuation(
      final ReplayingActivityReactor<T, Event, Activity> reactor,
      final String activityId,
      final Activity activity)
  {
    this(reactor, activityId, activity, TreePVector.empty());
  }

  public ActivityContinuation<T, Event, Activity> plus(final ActivityBreadcrumb<T, Event> breadcrumb) {
    return new ActivityContinuation<>(this.reactor, this.activityId, this.activity, this.breadcrumbs.plus(breadcrumb));
  }

  @Override
  public String getId() {
    return this.activityId;
  }

  @Override
  public Pair<History<T, Event>, Map<String, ScheduleItem<T, Event>>> runFrom(final History<T, Event> history) {
    return this.reactor.react(history, this);
  }

  @Override
  public String toString() {
    return String.format("activity(step = %d, activity = %s", this.breadcrumbs.size(), this.activity);
  }
}
