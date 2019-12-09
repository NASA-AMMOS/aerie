package gov.nasa.jpl.ammos.mpsa.aerie.simulation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationContext;

public final class DevActivity implements Activity<DevStates> {
  @Override
  public void modelEffects(final SimulationContext ctx, final DevStates states) {
    final int tmp = states.dev.get() + states.dev2.get();
    states.dev.set(states.dev2.get());
    states.dev2.set(tmp);
  }
}
