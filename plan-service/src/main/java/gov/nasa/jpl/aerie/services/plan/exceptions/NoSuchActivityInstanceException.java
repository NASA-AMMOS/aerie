package gov.nasa.jpl.aerie.services.plan.exceptions;

public class NoSuchActivityInstanceException extends Exception {
  private final String planId;
  private final String activityInstanceId;

  public NoSuchActivityInstanceException(final String planId, final String activityInstanceId) {
    super("No activity exists with id `" + planId + "` in plan with id `" + planId + "`");
    this.planId = planId;
    this.activityInstanceId = activityInstanceId;
  }

  public String getInvalidActivityId() {
    return this.activityInstanceId;
  }

  public String getPlanId() {
    return this.planId;
  }
}
