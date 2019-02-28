import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * CommandParameter
 * <p>
 * 
 * 
 */
public class CommandParameter {

    /**
     * 
     * (Required)
     * 
     */
    private String defaultValue;
    private String help;
    /**
     * 
     * (Required)
     * 
     */
    private String name;
    private Double max;
    private Double min;
    /**
     * 
     * (Required)
     * 
     */
    private String range;
    private String regex;
    /**
     * 
     * (Required)
     * 
     */
    private String type;
    private String units;

    /**
     * 
     * (Required)
     * 
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
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

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getRange() {
        return range;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setRange(String range) {
        this.range = range;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
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

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("defaultValue", defaultValue).append("help", help).append("name", name).append("max", max).append("min", min).append("range", range).append("regex", regex).append("type", type).append("units", units).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(help).append(regex).append(min).append(max).append(defaultValue).append(name).append(range).append(units).append(type).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CommandParameter) == false) {
            return false;
        }
        CommandParameter rhs = ((CommandParameter) other);
        return new EqualsBuilder().append(help, rhs.help).append(regex, rhs.regex).append(min, rhs.min).append(max, rhs.max).append(defaultValue, rhs.defaultValue).append(name, rhs.name).append(range, rhs.range).append(units, rhs.units).append(type, rhs.type).isEquals();
    }

}
