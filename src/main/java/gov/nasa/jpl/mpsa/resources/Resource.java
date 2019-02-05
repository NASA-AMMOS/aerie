
package gov.nasa.jpl.mpsa.resources;

import gov.nasa.jpl.mpsa.time.Time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;


public class Resource<V extends Comparable> implements PropertyChangeListener{

    private UUID id;
    private String name;
    private String subsystem;
    private String units;
    private String interpolation;
    private Set allowedValues;
    private V minimum;
    private V maximum;
    private V value;


    // other resources or conditions might want to listen to when we change, so we will keep a collection of listeners

    // we want a Set instead of a List because we never ever want to notify the same thing that we've changed twice -
    // that could trigger duplicate events
    private Set<PropertyChangeListener> listeners;

    // if resource has been read in and we don't want to modify it, set frozen boolean
    private boolean frozen = false;

    // setting up main data structure that holds value history
    private List<V> resourceHistory = new ArrayList<V>();


    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public void setSubsystem(String subsystem) {
        this.subsystem = subsystem;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(String interpolation) {
        this.interpolation = interpolation;
    }

    public Set getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(Set allowedValues) {
        this.allowedValues = allowedValues;
    }

    public V getMinimum() {
        return minimum;
    }

    public void setMinimum(V minimum) {
        this.minimum = minimum;
    }

    public V getMaximum() {
        return maximum;
    }

    public void setMaximum(V maximum) {
        this.maximum = maximum;
    }

    public Set<PropertyChangeListener> getListeners() {
        return listeners;
    }

    public void setListeners(Set<PropertyChangeListener> listeners) {
        this.listeners = listeners;
    }


    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

    }


    public static class Builder<V extends Comparable> {

        private UUID id;
        private String name;
        private String subsystem;
        private String units;
        private String interpolation;
        private Set allowedValues;
        private V minimum;
        private V maximum;
        private boolean frozen;
        private V initialValue;

        public Builder(String name) {
            this.id = UUID.randomUUID();
            this.name = name;
        }

        public Builder withInitialValue(V value) {
            this.initialValue = value;
            return this;
        }

        public Builder forSubsystem(String subsystem) {
            this.subsystem = subsystem;
            return this;
        }

        public Builder withUnits(String units) {
            this.units = units;
            return this;
        }

        public Builder withInterpolation(String interpolation) {
            this.interpolation = interpolation;
            return this;
        }

        public Builder withAllowedValues(Set allowedValues) {
            this.allowedValues = allowedValues;
            return this;
        }

        public Builder withMin(V minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder withMax(V maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder isFrozen(boolean frozen) {
            this.frozen = frozen;
            return this;
        }

        public Resource build(){
            return new Resource(this);
        }


    }

    private Resource(Builder builder){
        this.id = builder.id;
        this.name = builder.name;
        this.subsystem = builder.subsystem;
        this.units = builder.units;
        this.interpolation = builder.interpolation;
        this.allowedValues = builder.allowedValues;
        this.minimum = (V) builder.minimum;
        this.maximum = (V) builder.maximum;
        this.frozen = builder.frozen;
        if (builder.initialValue != null) {
            this.setValue((V) builder.initialValue);
        }

        this.listeners = new HashSet<>();
        addChangeListener(this);
    }


    public void addChangeListener(PropertyChangeListener newListener) {
        listeners.add(newListener);
    }


    // needed to not schedule schedulers every time a remodel is run
    public void removeChangeListener(PropertyChangeListener toBeRemoved) {
        listeners.remove(toBeRemoved);
    }

    private void notifyListeners(Map.Entry<Time, V> oldValue, Map.Entry<Time, V> newValue) {
        for (PropertyChangeListener name : listeners) {
            name.propertyChange(new PropertyChangeEvent(this, "ResourceValue", oldValue, newValue));
        }
    }

    private void notifyListeners(V oldValue, V newValue) {
        for (PropertyChangeListener name : listeners) {
            name.propertyChange(new PropertyChangeEvent(this, "ResourceValue", oldValue, newValue));
        }
    }

    public void setName(String str) {
        name = str;
    }

    public boolean resourceHistoryHasElements() {
        return !resourceHistory.isEmpty();
    }

    public void clearHistory() { resourceHistory = new ArrayList<V>(); }

    public void setValue(V newValue) {

        // TODO: We want to implement a "dirty" flag that tells us if the value has changed.
        // This way, when we have to serialize this object to send it through the wire in a message, we minimize the
        // amount of data generated

        // if an activity tries to set a frozen resource, we don't know what to do right now, so just die
        if (this.frozen) {
            throw new RuntimeException("Tried to set frozen resource");
        }

        // now check if when assigning a value it is an allowed one
        if(this.allowedValues != null && this.allowedValues.contains(value)) {
            throw new RuntimeException("Value not allowed");
        }

        /*
        TODO: Discuss why we must rely on time. What if I just want a list of stuff ordered by "insertion" time.
        I get that when doing simulation it is easy to just pull from a table using the hash key (time)
        but makes it inflexible.
        */

        V oldValue = this.value;
        this.value = newValue;
      //  System.out.println("value is " + value);
    //    notifyListeners(new AbstractMap.SimpleImmutableEntry(0, 0), new AbstractMap.SimpleImmutableEntry(0, value));
        notifyListeners(oldValue, newValue);

        resourceHistory.add(value);

    }

    public V getCurrentValue() {
       /* if (resourceHistory.size()-1 < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        else return resourceHistory.get(resourceHistory.size() - 1);*/
       return value;
    }

    public List<V> getResourceHistory() {
        return resourceHistory;
    }

    @Override
    public String toString() {
        return this.id + " - " + this.name + " -> " + this.minimum + "/" + this.maximum + "(" + this.units + ")";
    }

}