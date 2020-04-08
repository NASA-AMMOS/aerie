package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.BiteBananaActivity$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.ParameterTestActivity$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.mappers.PeelBananaActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.CompositeActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.List;
import java.util.Map;

@Adaptation(name="Banananation", version="0.0.1")
public class Banananation implements MerlinAdaptation {
  private final ActivityMapper activityMapper = new CompositeActivityMapper(Map.of(
      "BiteBanana", new BiteBananaActivity$$ActivityMapper(),
      "PeelBanana", new PeelBananaActivityMapper(),
      "ParameterTest", new ParameterTestActivity$$ActivityMapper()
  ));

  @Override
  public ActivityMapper getActivityMapper() {
    return activityMapper;
  }

  @Override
  public StateContainer createStateModels() {
    return () -> List.of(BananaStates.fruitState, BananaStates.peelState);
  }

  static {
    SpiceLoader.loadSpice();
  }
}
