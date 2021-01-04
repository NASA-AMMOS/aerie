package gov.nasa.jpl.aerie.apgen.model;

public class ActivityInstanceParameter {
    private final String name;
    private final String type;
    private final String value;

    public ActivityInstanceParameter(String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getValue() {
        return this.value;
    }
}
