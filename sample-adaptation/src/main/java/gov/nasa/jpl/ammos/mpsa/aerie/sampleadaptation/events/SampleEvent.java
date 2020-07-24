package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.IndependentStateEvent;

import java.util.Objects;
import java.util.Optional;

public abstract class SampleEvent {
    private SampleEvent() {}

    public abstract <Result> Result visit(SampleEventHandler<Result> visitor);

    public static SampleEvent independent(final IndependentStateEvent event) {
        Objects.requireNonNull(event);
        return new SampleEvent() {
            @Override
            public <Result> Result visit(final SampleEventHandler<Result> visitor) {
                return visitor.independent(event);
            }
        };
    }

    public static SampleEvent activity(final ActivityEvent event) {
        Objects.requireNonNull(event);
        return new SampleEvent() {
            @Override
            public <Result> Result visit(final SampleEventHandler<Result> visitor) {
                return visitor.activity(event);
            }
        };
    }

    public final Optional<IndependentStateEvent> asIndependent() {
        return this.visit(new DefaultSampleEventHandler<>() {
            @Override
            public Optional<IndependentStateEvent> unhandled() {
                return Optional.empty();
            }

            @Override
            public Optional<IndependentStateEvent> independent(IndependentStateEvent event) {
                return Optional.of(event);
            }
        });
    }

    public final Optional<ActivityEvent> asActivity() {
        return this.visit(new DefaultSampleEventHandler<>() {
            @Override
            public Optional<ActivityEvent> unhandled() {
                return Optional.empty();
            }

            @Override
            public Optional<ActivityEvent> activity(final ActivityEvent event) {
                return Optional.of(event);
            }
        });
    }

    @Override
    public final String toString() {
        return this.visit(new SampleEventHandler<>() {
            @Override
            public String independent(final IndependentStateEvent event) {
                return String.format("independent.%s", event);
            }

            @Override
            public String activity(final ActivityEvent event) {
                return String.format("activity.%s", event);
            }
        });
    }
}
