package gov.nasa.jpl.plan.models;

import java.util.ArrayList;

public class ActivityInstance {
    public String activityId;
    public String activityType;
    public String color;
    public ArrayList<Constraint> constraints;
    public Number duration;
    public Number end;
    public String endTimestamp;
    public String intent;
    public ArrayList<String> listeners;
    public String name;
    public ArrayList<Parameter> parameters;
    public Number start;
    public String startTimestamp;
    public String y;

    public ActivityInstance() {}

    public ActivityInstance(
        String activityId,
        String activityType,
        String color,
        ArrayList<Constraint> constraints,
        Number duration,
        Number end,
        String endTimestamp,
        String intent,
        ArrayList<String> listeners,
        String name,
        ArrayList<Parameter> parameters,
        Number start,
        String startTimestamp,
        String y
    ) {
        this.activityId = activityId;
        this.activityType = activityType;
        this.color = color;
        this.constraints = constraints;
        this.duration = duration;
        this.end = end;
        this.endTimestamp = endTimestamp;
        this.intent = intent;
        this.listeners = listeners;
        this.name = name;
        this.parameters = parameters;
        this.start = start;
        this.startTimestamp = startTimestamp;
        this.y = y;
    }

    public String toString() {
        return
            "activityId: " + this.activityId + "\n" +
            "activityType: " + this.activityType + "\n" +
            "color: " + this.color + "\n" +
            "constraints: " + this.constraints.toString() + "\n" +
            "duration: " + this.color + "\n" +
            "end: " + this.end + "\n" +
            "endTimestamp: " + this.endTimestamp + "\n" +
            "intent: " + this.intent + "\n" +
            "listeners: " + this.listeners.toString() + "\n" +
            "name: " + this.name + "\n" +
            "parameters: " + this.parameters.toString() + "\n" +
            "start: " + this.start.toString() + "\n" +
            "startTimestamp: " + this.startTimestamp + "\n" +
            "y: " + this.y + "\n";
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public ArrayList<Constraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(ArrayList<Constraint> constraints) {
        this.constraints = constraints;
    }

    public Number getDuration() {
        return duration;
    }

    public void setDuration(Number duration) {
        this.duration = duration;
    }

    public Number getEnd() {
        return end;
    }

    public void setEnd(Number end) {
        this.end = end;
    }

    public String getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public ArrayList<String> getListeners() {
        return listeners;
    }

    public void setListeners(ArrayList<String> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Number getStart() {
        return start;
    }

    public void setStart(Number start) {
        this.start = start;
    }

    public String getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }
}
