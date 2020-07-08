package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEvent;

public interface DefaultSampleEventHandler<Result> extends SampleEventHandler<Result> {
    Result unhandled();

    @Override
    default Result independent(IndependentStateEvent event) {
        return unhandled();
    }
}
