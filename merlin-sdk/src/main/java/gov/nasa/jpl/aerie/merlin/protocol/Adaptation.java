package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public interface Adaptation<$Schema> {
  /* Produce */ Map<String, TaskSpecType<$Schema, ?>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ Iterable<Pair<String, Map<String, SerializedValue>>>
  /* Given   */ getDaemons();

  /* Produce */ Iterable<ResourceFamily<$Schema, ?, ?>>
  /* Given   */ getResourceFamilies();

  /* Produce */ Map<String, CompoundCondition<$Schema>>
  /* Given   */ getConstraints();

  /* Produce */ Schema<$Schema>
  /* Given   */ getSchema();
}
