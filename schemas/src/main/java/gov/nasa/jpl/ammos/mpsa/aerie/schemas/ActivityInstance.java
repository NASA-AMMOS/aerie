
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.util.ArrayList;
import java.util.List;


/**
 * ActivityInstance
 * <p>
 * 
 * 
 */
public class ActivityInstance {

    /**
     * 
     * (Required)
     * 
     */
    private String activityId;
    /**
     * 
     * (Required)
     * 
     */
    private String activityType;
    /**
     * 
     * (Required)
     * 
     */
    private String color;
    /**
     * 
     * (Required)
     * 
     */
    private List<ActivityInstanceConstraint> constraints = new ArrayList<ActivityInstanceConstraint>();
    /**
     * 
     * (Required)
     * 
     */
    private Double duration;
    /**
     * 
     * (Required)
     * 
     */
    private Double end;
    /**
     * 
     * (Required)
     * 
     */
    private String endTimestamp;
    /**
     * 
     * (Required)
     * 
     */
    private String intent;
    private List<String> listeners = new ArrayList<String>();
    /**
     * 
     * (Required)
     * 
     */
    private String name;
    /**
     * 
     * (Required)
     * 
     */
    private List<ActivityInstanceParameter> parameters = new ArrayList<ActivityInstanceParameter>();
    /**
     * 
     * (Required)
     * 
     */
    private Double start;
    /**
     * 
     * (Required)
     * 
     */
    private String startTimestamp;
    /**
     * 
     * (Required)
     * 
     */
    private Double y;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ActivityInstance() {
    }

    /**
     * 
     * @param color
     * @param listeners
     * @param start
     * @param constraints
     * @param intent
     * @param duration
     * @param activityId
     * @param name
     * @param y
     * @param end
     * @param activityType
     * @param endTimestamp
     * @param parameters
     * @param startTimestamp
     */
    public ActivityInstance(String activityId, String activityType, String color, List<ActivityInstanceConstraint> constraints, Double duration, Double end, String endTimestamp, String intent, List<String> listeners, String name, List<ActivityInstanceParameter> parameters, Double start, String startTimestamp, Double y) {
        super();
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

    /**
     * 
     * (Required)
     * 
     */
    public String getActivityId() {
        return activityId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getActivityType() {
        return activityType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getColor() {
        return color;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * 
     * (Required)
     * 
     */
    public List<ActivityInstanceConstraint> getConstraints() {
        return constraints;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setConstraints(List<ActivityInstanceConstraint> constraints) {
        this.constraints = constraints;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getDuration() {
        return duration;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setDuration(Double duration) {
        this.duration = duration;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getEnd() {
        return end;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setEnd(Double end) {
        this.end = end;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getEndTimestamp() {
        return endTimestamp;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getIntent() {
        return intent;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<String> getListeners() {
        return listeners;
    }

    public void setListeners(List<String> listeners) {
        this.listeners = listeners;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * (Required)
     * 
     */
    public List<ActivityInstanceParameter> getParameters() {
        return parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setParameters(List<ActivityInstanceParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getStart() {
        return start;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStart(Double start) {
        this.start = start;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getStartTimestamp() {
        return startTimestamp;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getY() {
        return y;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setY(Double y) {
        this.y = y;
    }

}
