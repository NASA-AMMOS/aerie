package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events.Event;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import org.pcollections.PVector;

public abstract class SchedulingEvent<T> {
  private SchedulingEvent() {}

  public static class InstantiateActivity<T> extends SchedulingEvent<T> {
    public final String activityId;
    public final String activityType;

    public InstantiateActivity(final String activityId, final String activityType) {
      this.activityId = activityId;
      this.activityType = activityType;
    }

    @Override
    public String toString() {
      return String.format("InstantiateActivity(id: \"%s\", type: \"%s\")",
          this.activityId.replace("\\", "\\\\").replace("\"", "\\\""),
          this.activityType.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }

  public static class ResumeActivity<T> extends SchedulingEvent<T> {
    public final String activityId;
    public final PVector<Time<T, Event>> milestones;

    public ResumeActivity(final String activityId, final PVector<Time<T, Event>> milestones) {
      this.activityId = activityId;
      this.milestones = milestones;
    }

    @Override
    public String toString() {
      return String.format("ResumeActivity(id: \"%s\")", this.activityId.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }
}
