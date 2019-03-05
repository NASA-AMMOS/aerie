
package gov.nasa.jpl.aerie.schemas;



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
    private Double max;
    private Double min;
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
     * No args constructor for use in serialization
     * 
     */
    public CommandParameter() {
    }

    /**
     * 
     * @param help
     * @param regex
     * @param min
     * @param max
     * @param defaultValue
     * @param name
     * @param range
     * @param units
     * @param type
     */
    public CommandParameter(String defaultValue, String help, Double max, Double min, String name, String range, String regex, String type, String units) {
        super();
        this.defaultValue = defaultValue;
        this.help = help;
        this.max = max;
        this.min = min;
        this.name = name;
        this.range = range;
        this.regex = regex;
        this.type = type;
        this.units = units;
    }

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

}
