package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

public interface Initializer<$Schema> {
  <CellType> CellType getInitialState(Query<? super $Schema, ?, ? extends CellType> query);

  <Event, Effect, CellType>
  Query<$Schema, Event, CellType>
  allocate(CellType initialState, Applicator<Effect, CellType> applicator, Projection<Event, Effect> projection);

  String daemon(TaskFactory<$Schema> factory);

  <Dynamics> void resourceFamily(ResourceFamily<$Schema, Dynamics> resourceFamily);

  interface TaskFactory<$Schema> {
    <$Timeline extends $Schema> Task<$Timeline> create();
  }
}
