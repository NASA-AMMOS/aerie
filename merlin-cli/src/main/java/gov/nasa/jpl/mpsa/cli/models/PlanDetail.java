package gov.nasa.jpl.mpsa.cli.models;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.jpl.aerie.schemas.ActivityInstance;
import gov.nasa.jpl.aerie.schemas.Plan;

public class PlanDetail extends Plan {
    private List<ActivityInstance> activityInstances = new ArrayList<ActivityInstance>();

    public PlanDetail() {
    }

    public PlanDetail(String _id, String adaptationId, String endTimestamp, String name, String startTimestamp,
            ArrayList<ActivityInstance> activityInstances) {
        super(_id, adaptationId, endTimestamp, name, startTimestamp);
        this.setActivityInstances(activityInstances);
    }

    public List<ActivityInstance> getActivityInstances() {
        return activityInstances;
    }

    public void setActivityInstances(List<ActivityInstance> activityInstances) {
        this.activityInstances = activityInstances;
    }

}
