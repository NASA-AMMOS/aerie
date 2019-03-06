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
    private Boolean selected;
    /**
     * 
     * (Required)
     * 
     */
    private String version;

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
    public Boolean getSelected() {
        return selected;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setSelected(Boolean selected) {
        this.selected = selected;
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
        return new ToStringBuilder(this).append("id", id).append("name", name).append("selected", selected).append("version", version).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(id).append(version).append(selected).toHashCode();
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
        return new EqualsBuilder().append(name, rhs.name).append(id, rhs.id).append(version, rhs.version).append(selected, rhs.selected).isEquals();
    }

}
