package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import org.pcollections.PVector;

public final class ResumeActivityEvent<T, Activity, Event> {
  public final String activityId;
  public final Activity activityType;
  public final PVector<ActivityBreadcrumb<T, Event>> milestones;

  public ResumeActivityEvent(final String activityId, final Activity activityType, final PVector<ActivityBreadcrumb<T, Event>> milestones) {
    this.activityId = activityId;
    this.activityType = activityType;
    this.milestones = milestones;
  }

  @Override
  public String toString() {
    return String.format("ResumeActivity(id: \"%s\", type: %s, step: %d)",
        this.activityId.replace("\\", "\\\\").replace("\"", "\\\""),
        this.activityType,
        this.milestones.size());
  }
}
