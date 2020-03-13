package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;

class ActivityJob {
    public enum ActivityStatus { NotStarted, InProgress, Complete }
    public final Activity<?> activity;
    public final ControlChannel channel = new ControlChannel();
    public ActivityStatus status = ActivityStatus.NotStarted;

    public ActivityJob(final Activity<?> activityInstance) {
        this.activity = activityInstance;
    }
}
