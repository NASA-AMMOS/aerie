package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;

import java.util.*;

public final class ActivityType {
    public final String name;
    public final Map<String, ParameterSchema> parameters;
    public final Map<String, SerializedValue> defaults;

    public ActivityType(
        final String name,
        final Map<String, ParameterSchema> parameterSchema,
        final Map<String, SerializedValue> defaults
    ) {
        this.name = name;
        this.parameters = Map.copyOf(parameterSchema);
        this.defaults = Map.copyOf(defaults);
    }

    // SAFETY: If equals is overridden, then hashCode must also be overridden.
    @Override
    public boolean equals(final Object object) {
        if (object.getClass() != ActivityType.class) {
            return false;
        }

        final ActivityType other = (ActivityType)object;
        return
                (  Objects.equals(this.name, other.name)
                && Objects.equals(this.parameters, other.parameters)
                && Objects.equals(this.defaults, other.defaults)
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.parameters, this.defaults);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
            + " {name=\"" + this.name + "\""
            + ", parameters=" + this.parameters
            + ", defaults=" + this.defaults
            + "}";
    }
}
