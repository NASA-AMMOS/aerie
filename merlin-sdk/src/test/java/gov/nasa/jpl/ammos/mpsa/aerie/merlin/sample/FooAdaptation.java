package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities.ActivityInstance;

import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooAdaptation implements Adaptation<ActivityInstance> {
  @Override
  public Map<String, ActivityType<ActivityInstance>> getActivityTypes() {
    return ActivityInstance.getActivityTypes();
  }

  @Override
  public SimulationScope<?, ?, ActivityInstance> createSimulationScope() {
    return FooSimulationScope.create();
  }
}
