package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.SimulationState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.CompositeActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.DownlinkData$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.InitializeBinDataVolume$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOff$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOn$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Adaptation(name="sample-adaptation", version="0.1")
public class SampleAdaptation implements MerlinAdaptation {
    private final ActivityMapper activityMapper = new CompositeActivityMapper(Map.of(
            "DownlinkData", new DownlinkData$$ActivityMapper(),
            "InitializeBinDataVolume", new InitializeBinDataVolume$$ActivityMapper(),
            "TurnInstrumentOn", new TurnInstrumentOn$$ActivityMapper(),
            "TurnInstrumentOff", new TurnInstrumentOff$$ActivityMapper()
    ));

    @Override
    public ActivityMapper getActivityMapper() {
        return activityMapper;
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
