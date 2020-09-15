package gov.nasa.jpl.ammos.mpsa.aerie.banananation;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.AbstractMerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ViolableConstraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice.SpiceLoader;

import java.util.List;

@Adaptation(name = "Banananation", version = "0.0.1")
public class Banananation extends AbstractMerlinAdaptation<BananaEvent> {
  static {
    SpiceLoader.loadSpice();
  }

  @Override
  public List<ViolableConstraint> getViolableConstraints() {
    return BananaStates.violableConstraints;
  }

  @Override
  public <T> Querier<T, BananaEvent> makeQuerier(final SimulationTimeline<T, BananaEvent> database) {
    return new BananaQuerier<>(this.getActivityMapper(), database);
  }
}
