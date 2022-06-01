package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.function.Function;

public interface Initializer {
  <CellType> CellType getInitialState(Query<CellType> query);

  <Event, Effect, CellType>
  Query<CellType> allocate(
      CellType initialState,
      Applicator<Effect, CellType> applicator,
      EffectTrait<Effect> trait,
      Function<Event, Effect> projection,
      Topic<Event> topic);

  <Return> String daemon(TaskFactory<Return> factory);

  void resource(String name, Resource<?> resource);

  <Event> void topic(String name, Topic<Event> topic, ValueSchema schema, Function<Event, SerializedValue> serializer);

  interface TaskFactory<Return> {
    Task<Return> create();
  }
}
