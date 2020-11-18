package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface SimulationScope<$Schema, AdaptationTaskSpec extends TaskSpec> {
  /* Produce */ Schema<$Schema>
  /* Given   */ getSchema();

  /* Produce */ Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<? extends $Schema>, SerializedValue>>>
  /* Given   */ getDiscreteResources();

  /* Produce */ Map<String, ? extends Resource<History<? extends $Schema>, RealDynamics>>
  /* Given   */ getRealResources();

  /* For all */ <$Timeline extends $Schema>
  /* Produce */ Task<$Timeline, AdaptationTaskSpec>
  /* Given   */ createTask(AdaptationTaskSpec taskSpec);
}
