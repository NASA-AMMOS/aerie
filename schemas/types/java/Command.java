import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * Command
 * <p>
 * 
 * 
 */
public class Command {

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
    private List<CommandParameter> parameters = new ArrayList<CommandParameter>();
    private String template;

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
    public List<CommandParameter> getParameters() {
        return parameters;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setParameters(List<CommandParameter> parameters) {
        this.parameters = parameters;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("parameters", parameters).append("template", template).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(template).append(parameters).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Command) == false) {
            return false;
        }
        Command rhs = ((Command) other);
        return new EqualsBuilder().append(name, rhs.name).append(template, rhs.template).append(parameters, rhs.parameters).isEquals();
    }

}
