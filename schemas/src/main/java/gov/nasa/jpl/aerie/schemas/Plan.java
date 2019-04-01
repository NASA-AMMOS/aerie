
package gov.nasa.jpl.aerie.schemas;



/**
 * Plan
 * <p>
 * 
 * 
 */
public class Plan {

    /**
     * 
     * (Required)
     * 
     */
    private String adaptationId;
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
    private String id;
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
    private String startTimestamp;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Plan() {
    }

    /**
     * 
     * @param adaptationId
     * @param name
     * @param id
     * @param endTimestamp
     * @param startTimestamp
     */
    public Plan(String adaptationId, String endTimestamp, String id, String name, String startTimestamp) {
        super();
        this.adaptationId = adaptationId;
        this.endTimestamp = endTimestamp;
        this.id = id;
        this.name = name;
        this.startTimestamp = startTimestamp;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getAdaptationId() {
        return adaptationId;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setAdaptationId(String adaptationId) {
        this.adaptationId = adaptationId;
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
    public String getId() {
        return id;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setId(String id) {
        this.id = id;
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

}
