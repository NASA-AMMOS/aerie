package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Parameter;
import java.util.List;
import java.util.UUID;

// TODO: Possibly use or extend schemas.ActivityType instead
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.operations.AdaptationModel;

public class ActivityTypeBuilder {

    private ActivityType _activityType;

    public ActivityTypeBuilder() {
        _activityType = new ActivityType();
    }

    public ActivityTypeBuilder withName(String name) {
        _activityType.setName(name);
        return this;
    }

    public ActivityTypeBuilder withId(String id) {
        UUID uuid = UUID.fromString(id);
        _activityType.setId(uuid);
        return this;
    }

    public ActivityTypeBuilder withModel(AdaptationModel model) {
        _activityType.setModel(model);
        return this;
    }

    public ActivityTypeBuilder addRelationship(ActivityTypeBuilder activityType) {
        _activityType.getRelationships().add(activityType.getActivityType());
        return this;
    }

    public List<ActivityType> getRelationships() {
        return _activityType.getRelationships();
    }

    public ParameterBuilder createParameter() {
        ParameterBuilder parameter = new ParameterBuilder();
        _activityType.addParameter(parameter.getParameter());
        return parameter;
    }

    public List<Parameter> getParameters() {
        return _activityType.getParameters();
    }

    public ActivityType getActivityType() {
        return _activityType;
    }
}