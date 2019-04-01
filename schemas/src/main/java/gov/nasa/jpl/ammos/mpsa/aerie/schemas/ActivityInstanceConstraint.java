
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;



/**
 * ActivityInstanceConstraint
 * <p>
 * 
 * 
 */
public class ActivityInstanceConstraint {

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
