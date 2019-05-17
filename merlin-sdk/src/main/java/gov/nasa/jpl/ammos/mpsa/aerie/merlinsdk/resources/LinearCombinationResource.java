package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * This class extends Resource and allows for the definition of resources as the linear combination of other resources.
 * A canonical example is that "TotalPowerDraw" is the sum of "PowerDraw1" + "PowerDraw2". It can represent weighted
 * sums (such as when a unit conversion between input Resources is necessary) and can also be used for differences.
 * 
 * <p> The value of a LinearCombinationResource automatically changes when the value of one of its input Resources
 * changes and triggers a PropertyChangeEvent.
 */
public class LinearCombinationResource extends Resource implements PropertyChangeListener {

    /**
     * Stores the set of terms (Resources and their coefficients) that constitute the linear combination
     */
    private HashMap<Resource, Number> terms;

    public LinearCombinationResource() {
        super();
        terms = new HashMap<>();
        setValue(0.0);
    }

    /**
     * Adds a Resource object and associated coefficient to a HashMap for reference upon a property change, registers
     * this LinearCombinationResource as a PropertyChangeListener for the input Resource, and alters this
     * LinearCombinationResource's current value to reflect the new term in the linear combination.
     * 
     * @param resource the input Resource whose value forms part of the linear combination
     * @param coefficient the coefficient that should be applied to the input Resource's value
     * @throws IllegalArgumentException if the input Resource's value is not of a numeric type (e.g., a String)
     */
    public void addTerm(Resource resource, Number coefficient) {
        this.terms.put(resource, coefficient);
        resource.addChangeListener(this);
        try {
            double resourceValue = ((Number) resource.getCurrentValue()).doubleValue();
            setValue((double) getCurrentValue() + coefficient.doubleValue() * resourceValue);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Resource value must be a numeric type");
        } catch (NullPointerException e) {
            // TODO: enforce that resources do not have null values
            setValue(null);
        }
        // TODO: set the minimum and maximum values internally as a function of the input values
        // TODO: set the type internally based on input values
    }

    @Override
    /**
     * Recalculates the value of the LinearCombinationResource's current value after an input Resource's current value
     * changes. It avoids recalculating the entire linear combination by only calculating the delta and applying that
     * to the current value.
     * 
     * @param evt the PropertyChangeEvent triggered by a change in an input Resource's current value
     */
    public void propertyChange(PropertyChangeEvent evt) {
        final double oldResourceValue = ((Number) evt.getOldValue()).doubleValue();
        final double newResourceValue = ((Number) evt.getNewValue()).doubleValue();
        final double coefficient = terms.get((Resource) evt.getSource()).doubleValue();
        final double delta = coefficient * (newResourceValue - oldResourceValue);
        setValue((double) getCurrentValue() + delta);
    }

}