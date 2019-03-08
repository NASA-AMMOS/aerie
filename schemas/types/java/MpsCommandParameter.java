import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * MpsCommandParameter
 * <p>
 * 
 * 
 */
public class MpsCommandParameter {

    private String defaultValue;
    private String help;
    private String name;
    private String range;
    private String type;
    private String units;

    /**
     * No args constructor for use in serialization
     * 
     */
    public MpsCommandParameter() {
    }

    /**
     * 
     * @param help
     * @param defaultValue
     * @param name
     * @param range
     * @param units
     * @param type
     */
    public MpsCommandParameter(String defaultValue, String help, String name, String range, String type, String units) {
        super();
        this.defaultValue = defaultValue;
        this.help = help;
        this.name = name;
        this.range = range;
        this.type = type;
        this.units = units;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("defaultValue", defaultValue).append("help", help).append("name", name).append("range", range).append("type", type).append("units", units).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(help).append(defaultValue).append(name).append(range).append(units).append(type).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof MpsCommandParameter) == false) {
            return false;
        }
        MpsCommandParameter rhs = ((MpsCommandParameter) other);
        return new EqualsBuilder().append(help, rhs.help).append(defaultValue, rhs.defaultValue).append(name, rhs.name).append(range, rhs.range).append(units, rhs.units).append(type, rhs.type).isEquals();
    }

}
