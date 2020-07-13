package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapperLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.events.SampleEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleQuerier;

@Adaptation(name="sample-adaptation", version="0.1")
public class SampleAdaptation implements MerlinAdaptation<SampleEvent> {
    @Override
    public ActivityMapper getActivityMapper() {
        try {
            return ActivityMapperLoader.loadActivityMapper(SampleAdaptation.class);
        } catch (ActivityMapperLoader.ActivityMapperLoadException e) {
            throw new Error(e);
        }
    }

    @Override
    public <T> Querier<T, SampleEvent> makeQuerier(final SimulationTimeline<T, SampleEvent> database) {
        return new SampleQuerier<>(database);
    }
}
