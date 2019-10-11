package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;

import java.util.*;

public final class ActivityType {
    public String name;
    public Map<String, ParameterSchema> parameters;

    public ActivityType() {}

    public ActivityType(final String name, final Map<String, ParameterSchema> parameterSchema) {
        this.name = name;
        this.parameters = parameterSchema;
    }

    public ActivityType(final ActivityType template) {
        this.name = template.name;
        this.parameters = new HashMap<>(template.parameters);
    }

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
}
