package gov.nasa.jpl.aerie.merlin.server.exceptions;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

public class NoSuchActivityInstanceException extends Exception {
  private final PlanId planId;
  private final ActivityDirectiveId activityDirectiveId;

  public NoSuchActivityInstanceException(final PlanId planId, final ActivityDirectiveId activityDirectiveId) {
    super("No activity exists with id `" + activityDirectiveId + "` in plan with id `" + planId + "`");
    this.planId = planId;
    this.activityDirectiveId = activityDirectiveId;
  }

  public ActivityDirectiveId getInvalidActivityId() {
    return this.activityDirectiveId;
  }

  public PlanId getPlanId() {
    return this.planId;
  }
}
