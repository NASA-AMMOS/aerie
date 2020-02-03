package gov.nasa.jpl.ammos.mpsa.aerie.simulation.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.SettableState;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.Map;

public final class DevState implements SettableState<Integer> {
  private int value = 1;

  @Override
  public Integer get() {
    return this.value;
  }

  @Override
  public String getName() {
    return "dev";
  }

  @Override
  public Map<Instant, Integer> getHistory() {
    return Map.of();
  }

  @Override
  public void set(final Integer value) {
    this.value = value;
  }
}
