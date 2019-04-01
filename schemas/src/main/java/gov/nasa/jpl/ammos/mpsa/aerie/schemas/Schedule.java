
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;



/**
 * Schedule
 * <p>
 * 
 * 
 */
public class Schedule {

    private String id;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Schedule() {
    }

    /**
     * 
     * @param id
     */
    public Schedule(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
