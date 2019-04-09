
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.util.ArrayList;
import java.util.List;


/**
 * ActivityInstanceParameter
 * <p>
 * 
 * 
 */
public class ActivityInstanceParameter {

    private String defaultValue;
    /**
     * 
     * (Required)
     * 
     */
    private String name;
    private List<String> range = new ArrayList<String>();
    /**
     * 
     * (Required)
     * 
     */
    private String type;
    /**
     * 
     * (Required)
     * 
     */
    private String value;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ActivityInstanceParameter() {
    }

    /**
     * 
     * @param defaultValue
     * @param name
     * @param range
     * @param type
     * @param value
     */
    public ActivityInstanceParameter(String defaultValue, String name, List<String> range, String type, String value) {
        super();
        this.defaultValue = defaultValue;
        this.name = name;
        this.range = range;
        this.type = type;
        this.value = value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
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

    public List<String> getRange() {
        return range;
    }

    public void setRange(List<String> range) {
        this.range = range;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setValue(String value) {
        this.value = value;
    }

}
