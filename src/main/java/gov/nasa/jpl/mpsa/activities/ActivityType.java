package gov.nasa.jpl.mpsa.activities;

import gov.nasa.jpl.mpsa.activities.operations.AdaptationModel;
import gov.nasa.jpl.mpsa.resources.ResourcesContainer;
import gov.nasa.jpl.mpsa.time.Time;

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
        Parameter start = new Parameter.Builder("startTime")
                .withValue(startTime)
                .ofType(Time.class)
                .build();

        this.addParameter(start);
    }

    public void setModel(AdaptationModel model) {
        this.model = model;
    }

    public void executeModel() {
        this.model.setup();
        this.model.execute();
        // dispatch an event|message with the serialized resource container state:
        // sendMessge(ResourcesContainer.getInstance().serialize());

    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    public List<Parameter> showParameters() {
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

    public List<ActivityType> getRelationsips() {
        return relationsips;
    }

    public void setRelationsips(List<ActivityType> relationsips) {
        this.relationsips = relationsips;
    }
}
