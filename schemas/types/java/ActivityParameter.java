import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * ActivityParameter
 * <p>
 * 
 * 
 */
public class ActivityParameter {

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
    public ActivityParameter() {
    }

    /**
     * 
     * @param name
     * @param type
     */
    public ActivityParameter(String name, String type) {
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

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("type", type).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(type).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ActivityParameter) == false) {
            return false;
        }
        ActivityParameter rhs = ((ActivityParameter) other);
        return new EqualsBuilder().append(name, rhs.name).append(type, rhs.type).isEquals();
    }

}
