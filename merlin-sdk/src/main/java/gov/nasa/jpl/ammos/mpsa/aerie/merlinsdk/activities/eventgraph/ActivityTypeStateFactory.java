package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

public final class ActivityTypeStateFactory {
  private final ActivityModelQuerier querier;

  public ActivityTypeStateFactory(final ActivityModelQuerier querier) {
    this.querier = querier;
  }

  public ActivityTypeState ofType(final String activityType) {
    return new ActivityTypeState(activityType, this.querier);
  }
}
