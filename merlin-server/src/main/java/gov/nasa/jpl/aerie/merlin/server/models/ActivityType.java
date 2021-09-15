package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ActivityType {
    public final String name;
    public final List<TaskSpecType.Parameter> parameters;
    public final Map<String, SerializedValue> defaults;

    public ActivityType(
        final String name,
        final List<TaskSpecType.Parameter> parameters,
        final Map<String, SerializedValue> defaults
    ) {
        this.name = name;
        this.parameters = List.copyOf(parameters);
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
