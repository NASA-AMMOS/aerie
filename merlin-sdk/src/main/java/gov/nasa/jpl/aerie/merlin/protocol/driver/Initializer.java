package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.function.Function;

public interface Initializer {
  <CellType> CellType getInitialState(Query<?, ? extends CellType> query);

  <Event, Effect, CellType>
  Query<Event, CellType> allocate(
      CellType initialState,
      Applicator<Effect, CellType> applicator,
      EffectTrait<Effect> trait,
      Function<Event, Effect> projection);

  Scheduler.TaskIdentifier daemon(TaskFactory factory);

  void resource(String name, Resource<?> resource);

  <Event> void topic(String name, Query<Event, ?> query, ValueSchema schema, Function<Event, SerializedValue> serializer);

  interface TaskFactory {
    Task create();
  }
}
