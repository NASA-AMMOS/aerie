
package gov.nasa.jpl.aerie.schemas;



/**
 * Plan
 * <p>
 * 
 * 
 */
public class Plan {

    private String adaptationId;
    private String endTimestamp;
    private String id;
    private String name;
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

    public String getAdaptationId() {
        return adaptationId;
    }

    public void setAdaptationId(String adaptationId) {
        this.adaptationId = adaptationId;
    }

    public String getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

}
