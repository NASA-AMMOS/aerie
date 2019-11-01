package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import java.nio.file.Path;
import java.util.Objects;

public final class Adaptation {
    public String name;
    public String version;
    public String mission;
    public String owner;

    /**
     * The path to the Adaptation JAR
     *
     * File at this location should not
     * be deleted except by its owner
     */
    public Path path;

    public Adaptation() {}

    public Adaptation(final Adaptation other) {
        this.name = other.name;
        this.version = other.version;
        this.mission = other.mission;
        this.owner = other.owner;
        this.path = other.path;
    }

    @Override
    public boolean equals(final Object object) {
        if (object.getClass() != Adaptation.class) {
            return false;
        }

        final Adaptation other = (Adaptation)object;
        return
                (  Objects.equals(this.name, other.name)
                && Objects.equals(this.version, other.version)
                && Objects.equals(this.mission, other.mission)
                && Objects.equals(this.owner, other.owner)
                && Objects.equals(this.path, other.path)
                );
    }
}
