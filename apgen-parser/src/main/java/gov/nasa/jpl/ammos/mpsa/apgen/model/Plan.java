package gov.nasa.jpl.ammos.mpsa.apgen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Plan {

    private final List<ActivityInstance> activityInstances;

    public Plan() {
        this.activityInstances = new ArrayList<>();
    }

    public Plan(List<ActivityInstance> activityInstances) {
        this.activityInstances = activityInstances;
    }

    public List<ActivityInstance> getActivityInstanceList() {
        return this.activityInstances;
    }

    public ActivityInstance getActivityInstance(String id) {
        for (ActivityInstance activityInstance : activityInstances) {
            if (activityInstance.getId().equals(id)) return activityInstance;
        }
        return null;
    }

    public void addActivityInstance(ActivityInstance activityInstance) {
        Objects.requireNonNull(activityInstance);
        activityInstances.add(activityInstance);
    }
}
