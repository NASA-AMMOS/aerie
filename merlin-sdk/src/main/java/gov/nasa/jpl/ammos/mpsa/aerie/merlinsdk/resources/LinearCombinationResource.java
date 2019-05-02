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
        setType(Double.class);
        // TODO: set min/max based upon input resources
    }

    public void addTerm(Resource resource, Number coefficient) {
        this.terms.put(resource, coefficient);
        resource.addChangeListener(this);
        double resourceValue;
        try {
            resourceValue = ((Number) resource.getCurrentValue()).doubleValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Resource value must be a Numeric type");
        }
        setValue((double) getCurrentValue() + resourceValue);
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