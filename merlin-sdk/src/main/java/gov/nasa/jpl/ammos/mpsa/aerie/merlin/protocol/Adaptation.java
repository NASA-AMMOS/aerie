package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface Adaptation<$Schema> {
  /* Produce */ Map<String, TaskSpecType<$Schema, ?>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ Iterable<Pair<String, Map<String, SerializedValue>>>
  /* Given   */ getDaemons();

  /* Produce */ Map<String, Pair<ValueSchema, Resource<$Schema, SerializedValue>>>
  /* Given   */ getDiscreteResources();

  /* Produce */ Map<String, Resource<$Schema, RealDynamics>>
  /* Given   */ getRealResources();

  /* Produce */ Schema<$Schema>
  /* Given   */ getSchema();
}
