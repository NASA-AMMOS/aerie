import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * CommandDictionary
 * <p>
 * 
 * 
 */
public class CommandDictionary {

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
    private String version;

    /**
     * No args constructor for use in serialization
     * 
     */
    public CommandDictionary() {
    }

    /**
     * 
     * @param name
     * @param id
     * @param version
     */
    public CommandDictionary(String id, String name, String version) {
        super();
        this.id = id;
        this.name = name;
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
        return new ToStringBuilder(this).append("id", id).append("name", name).append("version", version).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(id).append(version).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CommandDictionary) == false) {
            return false;
        }
        CommandDictionary rhs = ((CommandDictionary) other);
        return new EqualsBuilder().append(name, rhs.name).append(id, rhs.id).append(version, rhs.version).isEquals();
    }

}
