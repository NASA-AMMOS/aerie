package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;

import java.util.*;

public final class ActivityType {
    public String name;
    public List<ActivityTypeParameter> parameters;

    public ActivityType() {}

    public ActivityType(final String name, final ParameterSchema parameterSchema) {
        this.name = name;
        this.parameters = buildParameterList(parameterSchema);
    }

    public ActivityType(final ActivityType template) {
        this.name = template.name;
        this.parameters = new ArrayList<>();
        template.parameters.forEach(parameter -> parameters.add(new ActivityTypeParameter(parameter)));
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

    private List<ActivityTypeParameter> buildParameterList(final ParameterSchema parameterSchema) {
        final List<ActivityTypeParameter> parameters = new ArrayList<>();

        final Optional<Map<String, ParameterSchema>> parameterMapOpt = parameterSchema.asMap();

        if (parameterMapOpt.isEmpty()) return null;

        for (final var parameterEntry : parameterMapOpt.get().entrySet()) {
            final String parameterName = parameterEntry.getKey();
            final String parameterType = parameterEntry.getValue().match(new SchemaTypeNameVisitor());

            final ActivityTypeParameter typeParam = new ActivityTypeParameter();
            typeParam.name = parameterName;
            typeParam.type = parameterType;
            parameters.add(typeParam);
        }

        return parameters;
    }
}
