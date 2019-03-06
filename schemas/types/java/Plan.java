import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


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

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("adaptationId", adaptationId).append("endTimestamp", endTimestamp).append("id", id).append("name", name).append("startTimestamp", startTimestamp).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(adaptationId).append(id).append(endTimestamp).append(startTimestamp).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Plan) == false) {
            return false;
        }
        Plan rhs = ((Plan) other);
        return new EqualsBuilder().append(name, rhs.name).append(adaptationId, rhs.adaptationId).append(id, rhs.id).append(endTimestamp, rhs.endTimestamp).append(startTimestamp, rhs.startTimestamp).isEquals();
    }

}
