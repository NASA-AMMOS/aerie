package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Adaptation;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;

public class AdaptationBuilder {

    private Adaptation _adaptation;
    private List<ResourceBuilder> _resources;
    private List<ActivityTypeBuilder> _activityTypes;

    public AdaptationBuilder() {
        _adaptation = new Adaptation();
        _resources = new ArrayList<ResourceBuilder>();
        _activityTypes = new ArrayList<ActivityTypeBuilder>();
    }

    public AdaptationBuilder withName(String name) {
        _adaptation.setName(name);
        return this;
    }

    public AdaptationBuilder withId(String id) {
        _adaptation.setId(id);
        return this;
    }

    public AdaptationBuilder withMission(String mission) {
        _adaptation.setMission(mission);
        return this;
    }

    public AdaptationBuilder withVersion(String version) {
        _adaptation.setVersion(version);
        return this;
    }

    public Adaptation getAdaptation() {
        return _adaptation;
    }

    public ResourceBuilder createResource() {
        ResourceBuilder resource = new ResourceBuilder();
        _resources.add(resource);
        return resource;
    }

    public List<ResourceBuilder> getResources() {
        return _resources;
    }

    public ActivityTypeBuilder createActivityType() {
        ActivityTypeBuilder activityType = new ActivityTypeBuilder();
        _activityTypes.add(activityType);
        return activityType;
    }

    public List<ActivityTypeBuilder> getActivivityTypes() {
        return _activityTypes;
    }
}
