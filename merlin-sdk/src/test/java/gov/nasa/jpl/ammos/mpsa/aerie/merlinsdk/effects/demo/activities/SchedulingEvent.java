package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

public abstract class SchedulingEvent<T> {
  private SchedulingEvent() {}

  public static class InstantiateActivity<T> extends SchedulingEvent<T> {
    public final String activityId;
    public final String activityType;

    public InstantiateActivity(final String activityId, final String activityType) {
      this.activityId = activityId;
      this.activityType = activityType;
    }
  }

  public static class ResumeActivity<T> extends SchedulingEvent<T> {
    public final String activityId;

    public ResumeActivity(final String activityId) {
      this.activityId = activityId;
    }
  }
}
