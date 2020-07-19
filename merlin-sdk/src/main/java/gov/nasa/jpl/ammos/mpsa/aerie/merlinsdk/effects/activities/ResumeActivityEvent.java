package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import java.util.Objects;

public final class ResumeActivityEvent<Activity> {
  public final String activityId;
  public final Activity activity;

  public ResumeActivityEvent(final String activityId, final Activity activity) {
    this.activityId = Objects.requireNonNull(activityId);
    this.activity = Objects.requireNonNull(activity);
  }

  @Override
  public String toString() {
    return String.format("ResumeActivity(id: \"%s\", activity: %s)",
        this.activityId.replace("\\", "\\\\").replace("\"", "\\\""),
        this.activity);
  }
}
