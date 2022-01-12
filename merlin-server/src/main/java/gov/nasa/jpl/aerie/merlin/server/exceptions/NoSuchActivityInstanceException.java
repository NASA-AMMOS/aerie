package gov.nasa.jpl.aerie.merlin.server.exceptions;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

public class NoSuchActivityInstanceException extends Exception {
  private final PlanId planId;
  private final ActivityInstanceId activityInstanceId;

  public NoSuchActivityInstanceException(final PlanId planId, final ActivityInstanceId activityInstanceId) {
    super("No activity exists with id `" + activityInstanceId + "` in plan with id `" + planId + "`");
    this.planId = planId;
    this.activityInstanceId = activityInstanceId;
  }

  public ActivityInstanceId getInvalidActivityId() {
    return this.activityInstanceId;
  }

  public PlanId getPlanId() {
    return this.planId;
  }
}
