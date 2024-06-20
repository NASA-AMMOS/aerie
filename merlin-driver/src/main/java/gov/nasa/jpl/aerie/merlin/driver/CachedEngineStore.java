package gov.nasa.jpl.aerie.merlin.driver;

import java.util.List;

public interface CachedEngineStore {
  void save(final CheckpointSimulationDriver.CachedSimulationEngine cachedSimulationEngine,
            final SimulationEngineConfiguration configuration);
  List<CheckpointSimulationDriver.CachedSimulationEngine> getCachedEngines(
      final SimulationEngineConfiguration configuration);

  int capacity();
}
