package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import java.io.InputStream;
import java.util.Objects;

public final class NewAdaptation {
    public final String name;
    public final String version;
    public final String mission;
    public final String owner;
    public final InputStream jarSource;

    private NewAdaptation(final Builder builder) {
        this.name = Objects.requireNonNull(builder.name);
        this.version = Objects.requireNonNull(builder.version);
        this.mission = Objects.requireNonNull(builder.mission);
        this.owner = Objects.requireNonNull(builder.owner);
        this.jarSource = Objects.requireNonNull(builder.jarSource);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
        private String name = null;
        private String version = null;
        private String mission = null;
        private String owner = null;
        private InputStream jarSource = null;

        private Builder() {}

        public Builder setName(final String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        public Builder setVersion(final String version) {
            this.version = Objects.requireNonNull(version);
            return this;
        }

        public Builder setMission(final String mission) {
            this.mission = Objects.requireNonNull(mission);
            return this;
        }

        public Builder setOwner(final String owner) {
            this.owner = Objects.requireNonNull(owner);
            return this;
        }

        public Builder setJarSource(final InputStream jarSource) {
            this.jarSource = Objects.requireNonNull(jarSource);
            return this;
        }

        public NewAdaptation build() {
            final var result = new NewAdaptation(this);
            this.jarSource = null;
            return result;
        }
    }
}
