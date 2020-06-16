package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import org.pcollections.PVector;

public abstract class SchedulingEvent<T> {
  private SchedulingEvent() {}

  public static class ResumeActivity<T> extends SchedulingEvent<T> {
    public final String activityId;
    public final String activityType;
    public final PVector<ActivityBreadcrumb<T, Event>> milestones;

    public ResumeActivity(final String activityId, final String activityType, final PVector<ActivityBreadcrumb<T, Event>> milestones) {
      this.activityId = activityId;
      this.activityType = activityType;
      this.milestones = milestones;
    }

    @Override
    public String toString() {
      return String.format("ResumeActivity(id: \"%s\", type: \"%s\", step: %d)",
          this.activityId.replace("\\", "\\\\").replace("\"", "\\\""),
          this.activityType.replace("\\", "\\\\").replace("\"", "\\\""),
          this.milestones.size());
    }
  }
}
