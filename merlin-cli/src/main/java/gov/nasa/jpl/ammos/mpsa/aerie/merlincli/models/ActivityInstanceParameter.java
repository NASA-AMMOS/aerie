package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

public class ActivityInstanceParameter {

    private String name;
    private String value;

    public ActivityInstanceParameter() {
        this.name = null;
        this.value = null;
    }

    public ActivityInstanceParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
