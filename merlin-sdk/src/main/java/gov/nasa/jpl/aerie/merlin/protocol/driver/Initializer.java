package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;

public interface Initializer<$Schema> {
  <CellType> CellType getInitialState(Query<$Schema, ?, CellType> query);

  <Event, Effect, CellType>
  Query<$Schema, Event, CellType>
  allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator);

  String daemon(TaskFactory<$Schema> factory);

  <Activity> void taskSpecType(String name, TaskSpecType<$Schema, Activity> taskSpecType);

  <Dynamics> void resourceFamily(ResourceFamily<$Schema, Dynamics> resourceFamily);

  interface TaskFactory<$Schema> {
    <$Timeline extends $Schema> Task<$Timeline> create();
  }
}
