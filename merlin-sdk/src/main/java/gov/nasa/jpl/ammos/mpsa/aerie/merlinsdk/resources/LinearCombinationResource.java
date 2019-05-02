package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import com.google.common.collect.ImmutableMap;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class LinearCombinationResource extends Resource implements PropertyChangeListener {

    private ImmutableMap<Resource, ? extends Number> terms;

    public LinearCombinationResource(ImmutableMap<Resource, ? extends Number> terms) {
        super();
        this.terms = terms;
        for (Resource resource : terms.keySet()) {
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