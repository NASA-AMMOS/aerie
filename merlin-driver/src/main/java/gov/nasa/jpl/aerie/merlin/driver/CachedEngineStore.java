package gov.nasa.jpl.aerie.merlin.driver;

import java.util.List;

public interface CachedEngineStore {
  void save(final SimulationDriver.CachedSimulationEngine cachedSimulationEngine,
            final SimulationEngineConfiguration configuration);
  List<SimulationDriver.CachedSimulationEngine> getCachedEngines(
      final SimulationEngineConfiguration configuration);
}
