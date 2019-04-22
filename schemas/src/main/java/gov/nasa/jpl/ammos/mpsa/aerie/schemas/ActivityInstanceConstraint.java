
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;



/**
 * ActivityInstanceConstraint
 * <p>
 * 
 * 
 */
public class ActivityInstanceConstraint {

    /**
     * Name of the constraint
     * (Required)
     * 
     */
    private String name;
    /**
     * Type of the constraint
     * (Required)
     * 
     */
    private String type;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ActivityInstanceConstraint() {
    }

    /**
     * 
     * @param name
     * @param type
     */
    public ActivityInstanceConstraint(String name, String type) {
        super();
        this.name = name;
        this.type = type;
    }

    /**
     * Name of the constraint
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the constraint
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Type of the constraint
     * (Required)
     * 
     */
    public String getType() {
        return type;
    }

    /**
     * Type of the constraint
     * (Required)
     * 
     */
    public void setType(String type) {
        this.type = type;
    }

}
