package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEvent;

public interface SampleEventHandler<Result> {
    Result independent(IndependentStateEvent event);
    Result activity(ActivityEvent event);
}
