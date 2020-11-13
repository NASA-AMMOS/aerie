package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;

import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooAdaptation implements Adaptation<FooActivityInstance> {
  @Override
  public Map<String, ActivityType<FooActivityInstance>> getActivityTypes() {
    return FooActivityInstance.getActivityTypes();
  }

  @Override
  public SimulationScope<?, ?, FooActivityInstance> createSimulationScope() {
    return FooSimulationScope.create();
  }
}
