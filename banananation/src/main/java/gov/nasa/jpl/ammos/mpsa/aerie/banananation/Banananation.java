package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.BiteBananaActivity$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.PeelBananaActivity$$ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.CompositeActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;

import java.util.Map;

@Adaptation(name="Banananation", version="0.0.1")
public class Banananation implements MerlinAdaptation {
  private final ActivityMapper activityMapper = new CompositeActivityMapper(Map.of(
      "BiteBanana", new BiteBananaActivity$$ActivityMapper(),
      "PeelBanana", new PeelBananaActivity$$ActivityMapper()
  ));

  @Override
  public ActivityMapper getActivityMapper() {
    return activityMapper;
  }

  static {
    SpiceLoader.loadSpice();
  }
}
