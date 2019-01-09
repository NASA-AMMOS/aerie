package gov.nasa.jpl.plan.models;

import java.util.ArrayList;

public class ActivityInstance {
    public String id;
    public String activityType;
    public String name;
    public ArrayList<String> listeners;
    public ArrayList<Parameter> parameters;

    public ActivityInstance() {

    }

    public ActivityInstance(String id, String activityType, String name,
            ArrayList<String> listeners, ArrayList<Parameter> parameters) {
        this.id = id;
        this.activityType = activityType;
        this.name = name;
        this.listeners = listeners;
        this.parameters = parameters;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public ArrayList<String> getListeners() {
        return listeners;
    }

    public void setListeners(ArrayList<String> listeners) {
        this.listeners = listeners;
    }

    public ArrayList<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<Parameter> parameters) {
        this.parameters = parameters;
    }
}

