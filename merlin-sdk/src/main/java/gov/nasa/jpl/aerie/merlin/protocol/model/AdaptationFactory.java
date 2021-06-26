package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;

public interface AdaptationFactory {
  <$Schema> void instantiate(SerializedValue configuration, Builder<$Schema> builder);

  List<Parameter> getParameters();

  record Parameter(String name, ValueSchema schema) {}

  interface Builder<$Schema> {
    boolean isBuilt();

    <CellType> CellType getInitialState(Query<$Schema, ?, CellType> query);

    <Event, Effect, CellType>
    Query<$Schema, Event, CellType>
    allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator);

    String daemon(TaskFactory<$Schema> factory);

    <Activity> void taskSpecType(String name, TaskSpecType<$Schema, Activity> taskSpecType);

    <Dynamics> void resourceFamily(ResourceFamily<$Schema, Dynamics> resourceFamily);
  }

  interface TaskFactory<$Schema> {
    <$Timeline extends $Schema> Task<$Timeline> create();
  }
}
