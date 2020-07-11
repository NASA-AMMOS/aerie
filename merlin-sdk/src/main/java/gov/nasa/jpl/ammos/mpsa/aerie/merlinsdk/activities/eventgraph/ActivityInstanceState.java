package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;

import java.util.List;

public final class ActivityInstanceState {
  private final String activityId;
  private final ActivityModelQuerier querier;

  public ActivityInstanceState(final String activityId, final ActivityModelQuerier querier) {
    this.activityId = activityId;
    this.querier = querier;
  }

  public Constraint whenActive() {
    return Constraint.createActivityConstraint(activityId, () -> List.of(querier.getCurrentInstanceWindow(activityId)));
  }
}
