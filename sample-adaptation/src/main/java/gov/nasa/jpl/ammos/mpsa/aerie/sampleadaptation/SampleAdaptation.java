package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimulationState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapperLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReactionContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.Set;

@Adaptation(name="sample-adaptation", version="0.1")
public class SampleAdaptation implements MerlinAdaptation<Object> {
    @Override
    public ActivityMapper getActivityMapper() {
        try {
            return ActivityMapperLoader.loadActivityMapper(SampleAdaptation.class);
        } catch (ActivityMapperLoader.ActivityMapperLoadException e) {
            // TODO: We should add an exception to merlin-sdk that adaptations can
            //       throw to signify that loading the activity mapper failed
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public <T> Querier<T, Object> makeQuerier(final SimulationTimeline<T, Object> database) {
        return new Querier<>() {
            @Override
            public void runActivity(final ReactionContext<T, Activity, Object> ctx, final Activity activity) {
                // TODO: run this activity with `ctx` made available to any states that depend on it
            }

            @Override
            public Set<String> states() {
                return Set.of();
            }

            @Override
            public SerializedParameter getSerializedStateAt(final String name, final History<T, Object> history) {
                throw new Error("I have no states, why are you calling me?");
            }
        };
    }
}
