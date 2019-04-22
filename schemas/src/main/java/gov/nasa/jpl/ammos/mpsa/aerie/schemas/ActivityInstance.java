
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
     * ID of the activity instance
     * (Required)
     * 
     */
    private String activityId;
    /**
     * The name of the activity type that this instance is based on
     * (Required)
     * 
     */
    private String activityType;
    /**
     * Background color of the activity instance within an activity band
     * (Required)
     * 
     */
    private String backgroundColor;
    /**
     * List of constraints associated with the activity instance
     * (Required)
     * 
     */
    private List<ActivityInstanceConstraint> constraints = new ArrayList<ActivityInstanceConstraint>();
    /**
     * How long the activity instance lasts
     * (Required)
     * 
     */
    private Double duration;
    /**
     * When the activity instance ends, as a Unix timestamp
     * (Required)
     * 
     */
    private Double end;
    /**
     * When the activity instances ends, as an ISO 8601 formatted date string
     * (Required)
     * 
     */
    private String endTimestamp;
    /**
     * Description of the activity instance
     * (Required)
     * 
     */
    private String intent;
    /**
     * A list of listeners
     * 
     */
    private List<String> listeners = new ArrayList<String>();
    /**
     * Name of the activity instance
     * (Required)
     * 
     */
    private String name;
    /**
     * Parameters which augment the runtime behavior of the instance
     * (Required)
     * 
     */
    private List<ActivityInstanceParameter> parameters = new ArrayList<ActivityInstanceParameter>();
    /**
     * When the activity instance starts, as a Unix timestamp
     * (Required)
     * 
     */
    private Double start;
    /**
     * When the activity instances starts, as an ISO 8601 formatted date string
     * (Required)
     * 
     */
    private String startTimestamp;
    /**
     * Text color of the activity instance within an activity band
     * (Required)
     * 
     */
    private String textColor;
    /**
     * The y position of the activity instance within an activity band
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
     * @param backgroundColor
     * @param listeners
     * @param start
     * @param constraints
     * @param intent
     * @param textColor
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
    public ActivityInstance(String activityId, String activityType, String backgroundColor, List<ActivityInstanceConstraint> constraints, Double duration, Double end, String endTimestamp, String intent, List<String> listeners, String name, List<ActivityInstanceParameter> parameters, Double start, String startTimestamp, String textColor, Double y) {
        super();
        this.activityId = activityId;
        this.activityType = activityType;
        this.backgroundColor = backgroundColor;
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
        this.textColor = textColor;
        this.y = y;
    }

    /**
     * ID of the activity instance
     * (Required)
     * 
     */
    public String getActivityId() {
        return activityId;
    }

    /**
     * ID of the activity instance
     * (Required)
     * 
     */
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    /**
     * The name of the activity type that this instance is based on
     * (Required)
     * 
     */
    public String getActivityType() {
        return activityType;
    }

    /**
     * The name of the activity type that this instance is based on
     * (Required)
     * 
     */
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    /**
     * Background color of the activity instance within an activity band
     * (Required)
     * 
     */
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Background color of the activity instance within an activity band
     * (Required)
     * 
     */
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * List of constraints associated with the activity instance
     * (Required)
     * 
     */
    public List<ActivityInstanceConstraint> getConstraints() {
        return constraints;
    }

    /**
     * List of constraints associated with the activity instance
     * (Required)
     * 
     */
    public void setConstraints(List<ActivityInstanceConstraint> constraints) {
        this.constraints = constraints;
    }

    /**
     * How long the activity instance lasts
     * (Required)
     * 
     */
    public Double getDuration() {
        return duration;
    }

    /**
     * How long the activity instance lasts
     * (Required)
     * 
     */
    public void setDuration(Double duration) {
        this.duration = duration;
    }

    /**
     * When the activity instance ends, as a Unix timestamp
     * (Required)
     * 
     */
    public Double getEnd() {
        return end;
    }

    /**
     * When the activity instance ends, as a Unix timestamp
     * (Required)
     * 
     */
    public void setEnd(Double end) {
        this.end = end;
    }

    /**
     * When the activity instances ends, as an ISO 8601 formatted date string
     * (Required)
     * 
     */
    public String getEndTimestamp() {
        return endTimestamp;
    }

    /**
     * When the activity instances ends, as an ISO 8601 formatted date string
     * (Required)
     * 
     */
    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    /**
     * Description of the activity instance
     * (Required)
     * 
     */
    public String getIntent() {
        return intent;
    }

    /**
     * Description of the activity instance
     * (Required)
     * 
     */
    public void setIntent(String intent) {
        this.intent = intent;
    }

    /**
     * A list of listeners
     * 
     */
    public List<String> getListeners() {
        return listeners;
    }

    /**
     * A list of listeners
     * 
     */
    public void setListeners(List<String> listeners) {
        this.listeners = listeners;
    }

    /**
     * Name of the activity instance
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the activity instance
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Parameters which augment the runtime behavior of the instance
     * (Required)
     * 
     */
    public List<ActivityInstanceParameter> getParameters() {
        return parameters;
    }

    /**
     * Parameters which augment the runtime behavior of the instance
     * (Required)
     * 
     */
    public void setParameters(List<ActivityInstanceParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * When the activity instance starts, as a Unix timestamp
     * (Required)
     * 
     */
    public Double getStart() {
        return start;
    }

    /**
     * When the activity instance starts, as a Unix timestamp
     * (Required)
     * 
     */
    public void setStart(Double start) {
        this.start = start;
    }

    /**
     * When the activity instances starts, as an ISO 8601 formatted date string
     * (Required)
     * 
     */
    public String getStartTimestamp() {
        return startTimestamp;
    }

    /**
     * When the activity instances starts, as an ISO 8601 formatted date string
     * (Required)
     * 
     */
    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    /**
     * Text color of the activity instance within an activity band
     * (Required)
     * 
     */
    public String getTextColor() {
        return textColor;
    }

    /**
     * Text color of the activity instance within an activity band
     * (Required)
     * 
     */
    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    /**
     * The y position of the activity instance within an activity band
     * (Required)
     * 
     */
    public Double getY() {
        return y;
    }

    /**
     * The y position of the activity instance within an activity band
     * (Required)
     * 
     */
    public void setY(Double y) {
        this.y = y;
    }

}
