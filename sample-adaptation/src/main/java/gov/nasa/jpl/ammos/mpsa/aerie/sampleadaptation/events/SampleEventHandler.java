package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent.events.IndependentStateEvent;

public interface SampleEventHandler<Result> {
    Result independent(IndependentStateEvent event);
    Result activity(ActivityEvent event);
}
