package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import java.util.Map;

public interface Adaptation<AdaptationTaskSpec extends TaskSpec> {
  /* Produce */ Map<String, TaskSpecType<AdaptationTaskSpec>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ Iterable<AdaptationTaskSpec>
  /* Given   */ getDaemons();

  /* Produce */ SimulationScope<?, AdaptationTaskSpec>
  /* Given   */ createSimulationScope();
}
