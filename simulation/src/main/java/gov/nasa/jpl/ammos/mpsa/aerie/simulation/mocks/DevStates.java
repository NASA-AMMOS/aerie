package gov.nasa.jpl.ammos.mpsa.aerie.simulation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.List;

public final class DevStates implements StateContainer {
  public final DevState dev = new DevState();
  public final DevState dev2 = new DevState();

  @Override
  public List<State<?>> getStateList() {
    return List.of(dev, dev2);
  }
}
