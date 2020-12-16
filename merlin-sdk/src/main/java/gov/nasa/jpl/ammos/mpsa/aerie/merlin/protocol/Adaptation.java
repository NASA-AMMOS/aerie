package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface Adaptation<$Schema> {
  /* Produce */ Map<String, TaskSpecType<$Schema, ?>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ Iterable<Pair<String, Map<String, SerializedValue>>>
  /* Given   */ getDaemons();

  /* Produce */ Map<String, Pair<ValueSchema, DiscreteResource<$Schema, SerializedValue>>>
  /* Given   */ getDiscreteResources();

  /* Produce */ Map<String, RealResource<$Schema>>
  /* Given   */ getRealResources();

  /* Produce */ Schema<$Schema>
  /* Given   */ getSchema();
}
