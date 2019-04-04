
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;



/**
 * ActivityInstanceParameter
 * <p>
 * 
 * 
 */
public class ActivityInstanceParameter {

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
     * @param name
     * @param type
     * @param value
     */
    public ActivityInstanceParameter(String name, String type, String value) {
        super();
        this.name = name;
        this.type = type;
        this.value = value;
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
