package gov.nasa.jpl.ammos.mpsa.aerie.simulation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.CompositeActivityMapper;

import java.util.Map;

public final class DevAdaptation implements MerlinAdaptation<DevStates> {
  @Override
  public ActivityMapper getActivityMapper() {
    return new CompositeActivityMapper(Map.of());
  }

  @Override
  public DevStates createStateModels() {
    return new DevStates();
  }
}