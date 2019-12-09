package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import java.util.List;

public class ActivityInstance {

    private String activityType;
    private String startTimestamp;
    private String name;
    private List<ActivityInstanceParameter> parameters;

    public ActivityInstance() {
        this.activityType = null;
        this.startTimestamp = null;
        this.name = null;
        this.parameters = null;
    }

    public ActivityInstance(String activityType, String startTimestamp, String name) {
        this.activityType = activityType;
        this.startTimestamp = startTimestamp;
        this.name = name;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public String getStartTimestamp() {
        return startTimestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setParameters(List<ActivityInstanceParameter> parameters) {
        this.parameters = parameters;
    }

    public List<ActivityInstanceParameter> getParameters() {
        return List.copyOf(this.parameters);
    }
}
