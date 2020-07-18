package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.IndependentStateEvent;

public interface DefaultSampleEventHandler<Result> extends SampleEventHandler<Result> {
    Result unhandled();

    @Override
    default Result independent(final IndependentStateEvent event) {
        return unhandled();
    }

    @Override
    default Result activity(final ActivityEvent event) {
        return unhandled();
    }
}
