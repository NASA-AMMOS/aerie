package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import java.util.Map;

public interface Adaptation<$Schema, AdaptationTaskSpec extends TaskSpec> {
  /* Produce */ Map<String, TaskSpecType<AdaptationTaskSpec>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ Iterable<AdaptationTaskSpec>
  /* Given   */ getDaemons();

  /* Produce */ SimulationScope<$Schema, AdaptationTaskSpec>
  /* Given   */ createSimulationScope();
}
