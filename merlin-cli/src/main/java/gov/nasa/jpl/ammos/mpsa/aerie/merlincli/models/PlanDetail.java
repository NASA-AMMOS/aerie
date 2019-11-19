package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import java.util.ArrayList;
import java.util.List;

public class PlanDetail {
    private String _id;
    private String adaptationId;
    private String startTimestamp;
    private String name;
    private String endTimestamp;
    private List<ActivityInstance> activityInstances = new ArrayList<ActivityInstance>();

    public PlanDetail() {
    }

    public PlanDetail(String _id, String adaptationId, String endTimestamp, String name, String startTimestamp,
            ArrayList<ActivityInstance> activityInstances) {
        this._id = _id;
        this.adaptationId = adaptationId;
        this.startTimestamp = startTimestamp;
        this.name = name;
        this.endTimestamp = endTimestamp;
        this.setActivityInstances(activityInstances);
    }

    public List<ActivityInstance> getActivityInstances() {
        return activityInstances;
    }

    public void setActivityInstances(List<ActivityInstance> activityInstances) {
        this.activityInstances = activityInstances;
    }

    public String getID() {
        return this._id;
    }

    public void setID(String id) {
        this._id = id;
    }

    public String getAdaptationId() {
        return this.adaptationId;
    }

    public void setAdaptationId(String adaptationId) {
        this.adaptationId = adaptationId;
    }

    public String getStartTimestamp() {
        return this.startTimestamp;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndTimestamp() {
        return this.endTimestamp;
    }

    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }
}
