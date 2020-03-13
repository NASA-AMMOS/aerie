package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

class ActivityJob<T extends StateContainer> {
    public enum ActivityStatus { NotStarted, InProgress, Complete }

    public final Activity<T> activity;
    public final ControlChannel channel = new ControlChannel();
    public ActivityStatus status = ActivityStatus.NotStarted;

    public ActivityJob(final Activity<T> activityInstance) {
        this.activity = activityInstance;
    }
}
