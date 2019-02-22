import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * ActivityType
 * <p>
 * 
 * 
 */
public class ActivityType {

    /**
     * 
     * (Required)
     * 
     */
    private String activityClass;
    /**
     * 
     * (Required)
     * 
     */
    private List<String> listeners = new ArrayList<String>();
    /**
     * 
     * (Required)
     * 
     */
    private List<ActivityTypeParameter> parameters = new ArrayList<ActivityTypeParameter>();
    /**
     * 
     * (Required)
     * 
     */
    private String typeName;

    /**
     * 
     * (Required)
     * 
     */
    public String getActivityClass() {
        return activityClass;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setActivityClass(String activityClass) {
        this.activityClass = activityClass;
    }

    /**
     * 
     * (Required)
     * 
     */
    public List<String> getListeners() {
        return listeners;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setListeners(List<String> listeners) {
        this.listeners = listeners;
    }

    /**
     * 
     * (Required)
     * 
     */
    public List<ActivityTypeParameter> getParameters() {
        return parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setParameters(List<ActivityTypeParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("activityClass", activityClass).append("listeners", listeners).append("parameters", parameters).append("typeName", typeName).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(typeName).append(listeners).append(parameters).append(activityClass).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ActivityType) == false) {
            return false;
        }
        ActivityType rhs = ((ActivityType) other);
        return new EqualsBuilder().append(typeName, rhs.typeName).append(listeners, rhs.listeners).append(parameters, rhs.parameters).append(activityClass, rhs.activityClass).isEquals();
    }

}
