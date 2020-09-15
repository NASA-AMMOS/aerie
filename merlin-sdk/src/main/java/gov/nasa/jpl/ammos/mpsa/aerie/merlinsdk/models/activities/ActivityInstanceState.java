package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintStructure;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConditionTypes.ActivityCondition.OCCURRING;

public final class ActivityInstanceState {
  private final String activityId;
  private final String activityType;
  private final ActivityModelQuerier querier;

  public ActivityInstanceState(final String activityId, final String activityType, final ActivityModelQuerier querier) {
    this.activityId = activityId;
    this.activityType = activityType;
    this.querier = querier;
  }

  public Constraint whenActive() {
    return Constraint.createActivityConstraint(
        activityId,
        () -> new Windows(querier.getCurrentInstanceWindow(activityId)),
        ConstraintStructure.ofActivityConstraint(activityType, OCCURRING));
  }
}
