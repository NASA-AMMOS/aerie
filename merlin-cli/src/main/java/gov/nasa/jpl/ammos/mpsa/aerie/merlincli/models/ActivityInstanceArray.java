package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import java.util.List;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;

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
