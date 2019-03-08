import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * MpsCommand
 * <p>
 * 
 * 
 */
public class MpsCommand {

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
    private List<MpsCommandParameter> parameters = new ArrayList<MpsCommandParameter>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MpsCommand() {
    }

    /**
     * 
     * @param name
     * @param parameters
     */
    public MpsCommand(String name, List<MpsCommandParameter> parameters) {
        super();
        this.name = name;
        this.parameters = parameters;
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
    public List<MpsCommandParameter> getParameters() {
        return parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setParameters(List<MpsCommandParameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("parameters", parameters).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(parameters).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof MpsCommand) == false) {
            return false;
        }
        MpsCommand rhs = ((MpsCommand) other);
        return new EqualsBuilder().append(name, rhs.name).append(parameters, rhs.parameters).isEquals();
    }

}
