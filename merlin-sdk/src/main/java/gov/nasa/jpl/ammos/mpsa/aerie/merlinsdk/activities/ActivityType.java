package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.ParameterBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.operations.AdaptationModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityType {

    private UUID id;
    private List<Parameter> parameters= new ArrayList<Parameter>();
    private String name;
    private List<ActivityType> relationsips = new ArrayList<ActivityType>();

    private AdaptationModel model;

    public ActivityType() {}
    // Should we add a start time by default?
    public ActivityType(Time startTime) {
        Parameter start = new ParameterBuilder()
            .withName("startTime")
            .withValue(startTime)
            .ofType(Time.class)
            .getParameter();

        this.addParameter(start);
    }

    public void setModel(AdaptationModel model) {
        this.model = model;
    }

    public AdaptationModel getModel() {
        return this.model;
    }

    public void executeModel() {
        this.model.setup(parameters);
        this.model.execute();
        // dispatch an event|message with the serialized resource container state:
        // sendMessge(ResourcesContainer.getInstance().serialize());

    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public List<Parameter> getParameters() {
        return this.parameters;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ActivityType> getRelationships() {
        return relationsips;
    }

    public void setRelationships(List<ActivityType> relationsips) {
        this.relationsips = relationsips;
    }
}