package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface Adaptation<$Schema> {
  /* Produce */ Map<String, TaskSpecType<$Schema, ?>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ Iterable<Pair<String, Map<String, SerializedValue>>>
  /* Given   */ getDaemons();

  /* Produce */ Iterable<ResourceFamily<$Schema, ?, ?>>
  /* Given   */ getResourceFamilies();

  /* Produce */ Map<String, Condition<$Schema>>
  /* Given   */ getConstraints();

  /* Produce */ Schema<$Schema>
  /* Given   */ getSchema();
}
