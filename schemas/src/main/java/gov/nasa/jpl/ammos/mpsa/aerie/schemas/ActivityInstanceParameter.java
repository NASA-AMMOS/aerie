
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

    /**
     * Default value of the parameter
     * 
     */
    private String defaultValue;
    /**
     * Name of the parameter
     * (Required)
     * 
     */
    private String name;
    /**
     * A range of values, for instance min/max and enum
     * 
     */
    private List<String> range = new ArrayList<String>();
    /**
     * The type of this parameter
     * (Required)
     * 
     */
    private String type;
    /**
     * The value of the parameter
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

    /**
     * Default value of the parameter
     * 
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Default value of the parameter
     * 
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Name of the parameter
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the parameter
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * A range of values, for instance min/max and enum
     * 
     */
    public List<String> getRange() {
        return range;
    }

    /**
     * A range of values, for instance min/max and enum
     * 
     */
    public void setRange(List<String> range) {
        this.range = range;
    }

    /**
     * The type of this parameter
     * (Required)
     * 
     */
    public String getType() {
        return type;
    }

    /**
     * The type of this parameter
     * (Required)
     * 
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * The value of the parameter
     * (Required)
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * The value of the parameter
     * (Required)
     * 
     */
    public void setValue(String value) {
        this.value = value;
    }

}
