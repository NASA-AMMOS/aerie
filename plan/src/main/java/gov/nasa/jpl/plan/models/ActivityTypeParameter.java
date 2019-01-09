package gov.nasa.jpl.plan.models;

public class ActivityTypeParameter {
    private String type;
    private String name;

    public ActivityTypeParameter() {

    }

    public ActivityTypeParameter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
