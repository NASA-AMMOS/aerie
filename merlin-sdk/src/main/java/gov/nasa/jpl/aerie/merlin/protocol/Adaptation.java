package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.util.Map;

public interface Adaptation<$Schema> {
  /* Produce */ Map<String, TaskSpecType<$Schema, ?>>
  /* Given   */ getTaskSpecificationTypes();

  /* Produce */ <$Timeline extends $Schema> Task<$Timeline>
  /* Given   */ getDaemon();

  /* Produce */ Iterable<ResourceFamily<$Schema, ?, ?>>
  /* Given   */ getResourceFamilies();

  /* Produce */ Map<String, CompoundCondition<$Schema>>
  /* Given   */ getConstraints();

  /* Produce */ Schema<$Schema>
  /* Given   */ getSchema();
}
