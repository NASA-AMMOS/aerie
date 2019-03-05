import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * Adaptation
 * <p>
 * 
 * 
 */
public class Adaptation {

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
    private String location;
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
    private String mission;
    /**
     * 
     * (Required)
     * 
     */
    private String owner;
    /**
     * 
     * (Required)
     * 
     */
    private String version;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Adaptation() {
    }

    /**
     * 
     * @param owner
     * @param mission
     * @param name
     * @param location
     * @param id
     * @param version
     */
    public Adaptation(String id, String location, String name, String mission, String owner, String version) {
        super();
        this.id = id;
        this.location = location;
        this.name = name;
        this.mission = mission;
        this.owner = owner;
        this.version = version;
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
    public String getLocation() {
        return location;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setLocation(String location) {
        this.location = location;
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
    public String getMission() {
        return mission;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setMission(String mission) {
        this.mission = mission;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getOwner() {
        return owner;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getVersion() {
        return version;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("location", location).append("name", name).append("mission", mission).append("owner", owner).append("version", version).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(owner).append(mission).append(name).append(location).append(id).append(version).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Adaptation) == false) {
            return false;
        }
        Adaptation rhs = ((Adaptation) other);
        return new EqualsBuilder().append(owner, rhs.owner).append(mission, rhs.mission).append(name, rhs.name).append(location, rhs.location).append(id, rhs.id).append(version, rhs.version).isEquals();
    }

}
