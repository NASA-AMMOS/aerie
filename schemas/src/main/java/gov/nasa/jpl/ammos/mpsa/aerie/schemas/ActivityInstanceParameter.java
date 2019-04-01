
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
     * No args constructor for use in serialization
     * 
     */
    public ActivityInstanceParameter() {
    }

    /**
     * 
     * @param name
     * @param type
     */
    public ActivityInstanceParameter(String name, String type) {
        super();
        this.name = name;
        this.type = type;
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

}
