package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import java.nio.file.Path;
import java.util.Objects;

public final class NewAdaptation {
    public String name;
    public String version;
    public String mission;
    public String owner;
    public Path path;

    public NewAdaptation() {}

    public NewAdaptation(final NewAdaptation other) {
        this.name = other.name;
        this.version = other.version;
        this.mission = other.mission;
        this.owner = other.owner;
        this.path = other.path;
    }

    @Override
    public boolean equals(final Object object) {
        if (object.getClass() != NewAdaptation.class) {
            return false;
        }

        final NewAdaptation other = (NewAdaptation)object;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.name, other.name)
                && Objects.equals(this.version, other.version)
                && Objects.equals(this.mission, other.mission)
                && Objects.equals(this.owner, other.owner)
                && Objects.equals(this.path, other.path);
    }
}
