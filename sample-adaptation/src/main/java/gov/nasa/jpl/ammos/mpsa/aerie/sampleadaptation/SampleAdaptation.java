package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimulationState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapperLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Adaptation(name="sample-adaptation", version="0.1")
public class SampleAdaptation implements MerlinAdaptation {

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
    public SimulationState newSimulationState(final Instant startTime) {
        final var model = new Model(startTime);

        return new SimulationState() {
            @Override
            public void applyInScope(final Runnable scope) {
                SampleMissionStates.useModelsIn(model, scope);
            }

            @Override
            public Map<String, State<?>> getStates() {
                final var states = List.of(model.instrumentPower_W, model.instrumentData, model.dataBin);

                return states.stream().collect(Collectors.toMap(x -> x.getName(), x -> x));
            }
        };
    }
}
