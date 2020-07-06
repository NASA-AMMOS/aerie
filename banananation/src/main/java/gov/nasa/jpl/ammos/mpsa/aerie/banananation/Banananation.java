package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapperLoader;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;

@Adaptation(name="Banananation", version="0.0.1")
public class Banananation implements MerlinAdaptation<BananaEvent> {
  static { SpiceLoader.loadSpice(); }

  @Override
  public ActivityMapper getActivityMapper() {
    try {
      return ActivityMapperLoader.loadActivityMapper(Banananation.class);
    } catch (final ActivityMapperLoader.ActivityMapperLoadException ex) {
      throw new Error(ex);
    }
  }

  @Override
  public <T> Querier<T, BananaEvent> makeQuerier(final SimulationTimeline<T, BananaEvent> database) {
    return new BananaQuerier<>(database);
  }
}
