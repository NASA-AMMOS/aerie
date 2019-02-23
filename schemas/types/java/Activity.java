import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * Activity
 * <p>
 * 
 * 
 */
public class Activity {

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
    private List<ActivityConstraint> constraints = new ArrayList<ActivityConstraint>();
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
    private List<ActivityParameter> parameters = new ArrayList<ActivityParameter>();
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
    public List<ActivityConstraint> getConstraints() {
        return constraints;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setConstraints(List<ActivityConstraint> constraints) {
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
    public List<ActivityParameter> getParameters() {
        return parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setParameters(List<ActivityParameter> parameters) {
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

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("activityId", activityId).append("activityType", activityType).append("color", color).append("constraints", constraints).append("duration", duration).append("end", end).append("endTimestamp", endTimestamp).append("intent", intent).append("name", name).append("parameters", parameters).append("start", start).append("startTimestamp", startTimestamp).append("y", y).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(color).append(start).append(constraints).append(intent).append(duration).append(activityId).append(name).append(y).append(end).append(activityType).append(endTimestamp).append(parameters).append(startTimestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Activity) == false) {
            return false;
        }
        Activity rhs = ((Activity) other);
        return new EqualsBuilder().append(color, rhs.color).append(start, rhs.start).append(constraints, rhs.constraints).append(intent, rhs.intent).append(duration, rhs.duration).append(activityId, rhs.activityId).append(name, rhs.name).append(y, rhs.y).append(end, rhs.end).append(activityType, rhs.activityType).append(endTimestamp, rhs.endTimestamp).append(parameters, rhs.parameters).append(startTimestamp, rhs.startTimestamp).isEquals();
    }

}
