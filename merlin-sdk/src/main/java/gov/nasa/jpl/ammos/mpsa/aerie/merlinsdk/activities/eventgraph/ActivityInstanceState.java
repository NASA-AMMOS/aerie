package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

public final class ActivityInstanceState {
  private final String activityId;
  private final ActivityModelQuerier querier;

  public ActivityInstanceState(final String activityId, final ActivityModelQuerier querier) {
    this.activityId = activityId;
    this.querier = querier;
  }

  public Constraint whenActive() {
    return Constraint.createActivityConstraint(activityId, () -> new Windows(querier.getCurrentInstanceWindow(activityId)));
  }
}
