package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ActivityType {
    public final String name;
    public final List<Parameter> parameters;

    public ActivityType(
        final String name,
        final List<Parameter> parameters
    ) {
        this.name = name;
        this.parameters = List.copyOf(parameters);
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
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.parameters);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
            + " {name=\"" + this.name + "\""
            + ", parameters=" + this.parameters
            + "}";
    }
}
