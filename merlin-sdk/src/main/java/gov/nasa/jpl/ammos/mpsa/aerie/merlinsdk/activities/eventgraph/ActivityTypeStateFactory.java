package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;

public final class ActivityTypeStateFactory {
  private final ActivityModelQuerier querier;

  public ActivityTypeStateFactory(final ActivityModelQuerier querier) {
    this.querier = querier;
  }

  public ActivityTypeState ofType(final String activityType) {
    return new ActivityTypeState(activityType, this.querier);
  }

  public ActivityTypeState ofType(final Class<? extends Activity> activityType) {
    return this.ofType(activityType.getAnnotation(ActivityType.class).name());
  }
}
