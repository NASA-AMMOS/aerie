
package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Time;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Instant;
import java.util.*;


public class Resource implements PropertyChangeListener {

    private UUID id;
    private Object type; // Might need to implement the type as a Comparable (e.g. <V extends Comparable>)
    private String name;
    private String subsystem;
    private String units;
    /**
     * Define the interpolation approach between data points
     */
    private String interpolation;
    private Set allowedValues;
    private Object minimum; // TODO: Type checking?
    private Object maximum; // TODO: Type checking?
    private Object value; // TODO: Type checking?


    // other resources or conditions might want to listen to when we change, so we will keep a collection of listeners

    // we want a Set instead of a List because we never ever want to notify the same thing that we've changed twice -
    // that could trigger duplicate events
    private Set<PropertyChangeListener> listeners;

    // if resource has been read in and we don't want to modify it, set frozen boolean
    private boolean frozen = false;

    // setting up main data structure that holds value history
    // TODO: Can this be resolved to type instead of Object?
    private Map<Instant, Object> resourceHistory = new LinkedHashMap<>();

    public Resource() {
        this.id = UUID.randomUUID();
        this.listeners = new HashSet<>();
    }

    public UUID getId() {
        return id;
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
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

    public Object getMinimum() {
        return minimum;
    }

    public void setMinimum(Object minimum) {
        this.minimum = minimum;
    }

    public Object getMaximum() {
        return maximum;
    }

    public void setMaximum(Object maximum) {
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

    public void addChangeListener(PropertyChangeListener newListener) {
        listeners.add(newListener);
    }

    // needed to not schedule schedulers every time a remodel is run
    public void removeChangeListener(PropertyChangeListener toBeRemoved) {
        listeners.remove(toBeRemoved);
    }

    private void notifyListeners(Map.Entry<Time, Object> oldValue, Map.Entry<Time, Object> newValue) {
        for (PropertyChangeListener name : listeners) {
            name.propertyChange(new PropertyChangeEvent(this, "ResourceValue", oldValue, newValue));
        }
    }

    private void notifyListeners(Object oldValue, Object newValue) {
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

    public void clearHistory() { resourceHistory = new HashMap<>(); }

    public void setValue(Object newValue) {

        // TODO: We want to implement a "dirty" flag that tells us if the value has changed.
        // This way, when we have to serialize this object to send it through the wire in a message, we minimize the
        // amount of data generated

        // if an activity tries to set a frozen resource, we don't know what to do right now, so just die
        if (this.frozen) {
            throw new RuntimeException("Tried to set frozen resource");
        }

        // now check if when assigning a value it is an allowed one
        if(this.allowedValues != null && !this.allowedValues.contains(newValue)) {
            throw new RuntimeException("Value not allowed");
        }

        /*
        TODO: Discuss why we must rely on time. What if I just want a list of stuff ordered by "insertion" time.
        I get that when doing simulation it is easy to just pull from a table using the hash key (time)
        but makes it inflexible.
        */

        Object oldValue = this.value;
        this.value = newValue;
        // System.out.println("value is " + value);
        // notifyListeners(new AbstractMap.SimpleImmutableEntry(0, 0), new AbstractMap.SimpleImmutableEntry(0, value));
        notifyListeners(oldValue, newValue);
        resourceHistory.put(Instant.now(),value);

    }

    public Object getCurrentValue() {
        return value;
    }

    public Map<Instant, Object> getResourceHistory() {
        return resourceHistory;
    }

    @Override
    public String toString() {
        return this.id + " - " + this.name + " -> " + this.minimum + "/" + this.maximum + "(" + this.units + ")";
    }

}