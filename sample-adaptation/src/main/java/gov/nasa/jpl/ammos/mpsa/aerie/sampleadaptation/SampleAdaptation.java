package gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.CompositeActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.DownlinkData$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.data.InitializeBinDataVolume$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOff$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.activities.instrument.TurnInstrumentOn$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.sampleadaptation.states.SampleMissionStates;

import java.util.Map;

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
    public StateContainer createStateModels() {
        return new SampleMissionStates(new Config());
    }
}
