package gov.nasa.jpl.aerie.apgen.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class Adaptation {

    private final List<ActivityType> activityTypes;

    public Adaptation() {
        activityTypes = new ArrayList<>();
    }

    public Adaptation(List<ActivityType> activityTypes) {
        this.activityTypes = activityTypes;
    }

    public List<ActivityType> getActivityTypes() {
        return List.copyOf(this.activityTypes);
    }

    public ActivityType getActivityType(String name) {
        for (ActivityType activityType : activityTypes) {
            if (activityType.getName().equals(name)) return activityType;
        }
        return null;
    }

    public boolean hasActivityType(String name) {
        return this.getActivityType(name) != null;
    }

    public void addActivityType(ActivityType activityType) {
        Objects.requireNonNull(activityType);
        this.activityTypes.add(activityType);
    }
}
