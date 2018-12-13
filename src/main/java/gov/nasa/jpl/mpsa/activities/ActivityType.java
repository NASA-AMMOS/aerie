package gov.nasa.jpl.mpsa.activities;

import gov.nasa.jpl.mpsa.activities.operations.Operation;
import gov.nasa.jpl.mpsa.time.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityType {

    private UUID id;
    private List<Parameter> parameters= new ArrayList<Parameter>();
    private String name;
    private List<ActivityType> relationsips = new ArrayList<ActivityType>();
    private Operation operation;

    public ActivityType() {}
    // Should we add a start time by default?
    public ActivityType(Time startTime) {
        Parameter start = new Parameter.Builder("startTime")
                .withValue(startTime)
                .ofType(Time.class)
                .build();

        this.addParameter(start);
    }

    // This us used to do modeling, decomposition, expansion, etc...
    public void executeOperation(Operation operation){
        operation.doOperation();
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
