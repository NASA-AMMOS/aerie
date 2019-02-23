package gov.nasa.jpl.mpsa.cli.models;

import java.util.ArrayList;
import java.util.List;

public class ActivityInstance {
    public String activityId;
    public String activityType;
    public String color;
    public List<Constraint> constraints;
    public Number duration;
    public Number end;
    public String endTimestamp;
    public String intent;
    public List<String> listeners;
    public String name;
    public List<Parameter> parameters;
    public Number start;
    public String startTimestamp;
    public Number y;

    public ActivityInstance() {}

    public ActivityInstance(
        String activityId,
        String activityType,
        String color,
        List<Constraint> constraints,
        Number duration,
        Number end,
        String endTimestamp,
        String intent,
        ArrayList<String> listeners,
        String name,
        List<Parameter> parameters,
        Number start,
        String startTimestamp,
        Number y
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

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<Constraint> constraints) {
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

    public List<String> getListeners() {
        return listeners;
    }

    public void setListeners(List<String> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
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

    public Number getY() {
        return y;
    }

    public void setY(Number y) {
        this.y = y;
    }
}
