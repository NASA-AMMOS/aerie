package gov.nasa.jpl.aerie.merlin.server.exceptions;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;

public class NoSuchActivityInstanceException extends Exception {
  private final String planId;
  private final ActivityInstanceId activityInstanceId;

  public NoSuchActivityInstanceException(final String planId, final ActivityInstanceId activityInstanceId) {
    super("No activity exists with id `" + planId + "` in plan with id `" + planId + "`");
    this.planId = planId;
    this.activityInstanceId = activityInstanceId;
  }

  public ActivityInstanceId getInvalidActivityId() {
    return this.activityInstanceId;
  }

  public String getPlanId() {
    return this.planId;
  }
}
