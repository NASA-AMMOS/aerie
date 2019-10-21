package gov.nasa.jpl.ammos.mpsa.apgen.model;

public class ActivityTypeParameter {
    private final String name;
    private final String type;
    private final String defaultValue;
    // More fields to come, such as range, comments and units

    public ActivityTypeParameter(String name, String type, String defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getDefault() {
        return this.defaultValue;
    }
}
