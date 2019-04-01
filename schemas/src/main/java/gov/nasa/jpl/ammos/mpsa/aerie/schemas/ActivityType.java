
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.util.ArrayList;
import java.util.List;


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
     * No args constructor for use in serialization
     * 
     */
    public ActivityType() {
    }

    /**
     * 
     * @param listeners
     * @param activityClass
     * @param typeName
     * @param parameters
     */
    public ActivityType(String activityClass, List<String> listeners, List<ActivityTypeParameter> parameters, String typeName) {
        super();
        this.activityClass = activityClass;
        this.listeners = listeners;
        this.parameters = parameters;
        this.typeName = typeName;
    }

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

}
