package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import java.util.Objects;

public final class ActivityTypeParameter {
    public String name;
    public String type;

    public ActivityTypeParameter() {}

    public ActivityTypeParameter(final ActivityTypeParameter other) {
        this.name = other.name;
        this.type = other.type;
    }

    @Override
    public boolean equals(final Object object) {
        if (object.getClass() != ActivityTypeParameter.class) {
            return false;
        }

        final ActivityTypeParameter other = (ActivityTypeParameter)object;
        return
                (  Objects.equals(this.name, other.name)
                && Objects.equals(this.type, other.type)
                );
    }
}
