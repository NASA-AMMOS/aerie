package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.ParameterBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.operations.AdaptationModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public class ActivityType implements PropertyChangeListener {

    private UUID id;
    private List<Parameter> parameters= new ArrayList<Parameter>();
    private String name;
    private List<ActivityType> relationsips = new ArrayList<ActivityType>();
    private boolean signal;
    private double value;

    private AdaptationModel model;

    // we want a Set instead of a List because we never ever want to notify the same thing that we've changed twice -
    // that could trigger duplicate events
    private Set<PropertyChangeListener> listeners;

    public ActivityType() {
        this.listeners = new HashSet<>();
        this.signal = false;
        this.value = 0;
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

    public void setSignal(boolean signal){
        if (this.signal != signal){
            boolean temp = this.signal;
            this.signal = signal;
            notifyListeners(temp, this.signal);
        }
    }

    public void setValue(double value){
        double temp = this.value;
        this.value = value;
        notifyListeners(temp, this.value);
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

    public void setListeners(Set<PropertyChangeListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("\n\n\nIN ACTIVITY PROPERTY CHANGED!!!\n\n\n");
        //Use functional resources to pass in an actual function
    }



    public void addChangeListener(PropertyChangeListener newListener) {
        listeners.add(newListener);
    }

    // needed to not schedule schedulers every time a remodel is run
    public void removeChangeListener(PropertyChangeListener toBeRemoved) {
        listeners.remove(toBeRemoved);
    }

    private void notifyListeners(Object oldSignal, Object newSignal) {
        for (PropertyChangeListener name : listeners) {
            name.propertyChange(new PropertyChangeEvent(this, "ActivitySignal", oldSignal, newSignal));
        }
    }
}