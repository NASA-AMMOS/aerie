package gov.nasa.jpl.mpsa.cli.models;

import java.util.List;

public class ActivityInstanceArray {

    private List<ActivityInstance> activityInstances;

    public ActivityInstanceArray() {}

    public ActivityInstanceArray(List<ActivityInstance> activityInstances) {
        this.setActivityInstances(activityInstances);
    }


    public List<ActivityInstance> getActivityInstances() {
        return activityInstances;
    }

    public void setActivityInstances(List<ActivityInstance> activityInstances) {
        this.activityInstances = activityInstances;
    }
}
