package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public class LinearCombinationResource extends Resource implements PropertyChangeListener {

    private HashMap<Resource, Number> terms;

    public LinearCombinationResource() {
        super();
        terms = new HashMap<>();
        setValue(0.0);
        // TODO: determine if we should set the type internally or if this is not transparent to the user
        setType(Double.class);
    }

    public void addTerm(Resource resource, Number coefficient) {
        this.terms.put(resource, coefficient);
        resource.addChangeListener(this);
        double resourceValue;   // resource's current value
        // TODO: handle resources that do not have current values (and will throw NullPointerExceptions)
        try {
            resourceValue = ((Number) resource.getCurrentValue()).doubleValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Resource value must be a Numeric type");
        }
        setValue((double) getCurrentValue() + coefficient.doubleValue() * resourceValue);
        // TODO: determine if we should set the minimum and maximum values internally as a function of the input values
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final double oldResourceValue = ((Number) evt.getOldValue()).doubleValue();
        final double newResourceValue = ((Number) evt.getNewValue()).doubleValue();
        final double coefficient = terms.get((Resource) evt.getSource()).doubleValue();
        final double delta = coefficient * (newResourceValue - oldResourceValue);
        setValue((double) getCurrentValue() + delta);
    }

}