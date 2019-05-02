package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public class LinearCombinationResource extends Resource implements PropertyChangeListener {

    private HashMap<Resource, Number> terms;

    public LinearCombinationResource() {
        super();
        terms = new HashMap<>();
    }

    public void setTerms(HashMap<Resource, ? extends Number> terms) {
        for (Resource resource : terms.keySet()) {
            this.terms.put(resource, terms.get(resource));
            resource.addChangeListener(this);
        }
        setValue(computeSum());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final double oldResourceValue = ((Number) evt.getOldValue()).doubleValue();
        final double newResourceValue = ((Number) evt.getNewValue()).doubleValue();
        final double coefficient = terms.get((Resource) evt.getSource()).doubleValue();
        final double delta = coefficient * (newResourceValue - oldResourceValue);
        setValue((double) getCurrentValue() + delta);
    }

    private Double computeSum() {
        double sum = 0.0;
        for (Resource resource : terms.keySet()) {
            double coefficient = terms.get(resource).doubleValue();
            double resourceValue;
            try {
                resourceValue = ((Number) resource.getCurrentValue()).doubleValue();
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Resource value must be a Numeric type");
            }
            sum += coefficient * resourceValue;
        }
        return sum;
    }

}