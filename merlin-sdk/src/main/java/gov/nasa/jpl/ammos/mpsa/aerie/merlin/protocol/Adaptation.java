package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface Adaptation<$Schema, AdaptationTaskSpec extends TaskSpec> {
  /* Produce */ Map<String, TaskSpecType<AdaptationTaskSpec>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ Iterable<AdaptationTaskSpec>
  /* Given   */ getDaemons();

  /* Produce */ Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<? extends $Schema>, SerializedValue>>>
  /* Given   */ getDiscreteResources();

  /* Produce */ Map<String, ? extends Resource<History<? extends $Schema>, RealDynamics>>
  /* Given   */ getRealResources();

  /* Produce */ Schema<$Schema>
  /* Given   */ getSchema();

  /* For all */ <$Timeline extends $Schema>
  /* Produce */ Task<$Timeline>
  /* Given   */ createTask(AdaptationTaskSpec taskSpec);
}
