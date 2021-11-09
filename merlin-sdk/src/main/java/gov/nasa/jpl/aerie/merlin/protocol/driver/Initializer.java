package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;

import java.util.function.Function;

public interface Initializer<$Schema> {
  <CellType> CellType getInitialState(Query<? super $Schema, ?, ? extends CellType> query);

  <Event, Effect, CellType>
  Query<$Schema, Event, CellType> allocate(
      CellType initialState,
      Applicator<Effect, CellType> applicator,
      EffectTrait<Effect> trait,
      Function<Event, Effect> projection);

  String daemon(TaskFactory<$Schema> factory);

  void resource(String name, Resource<? super $Schema, ?> resource);

  interface TaskFactory<$Schema> {
    <$Timeline extends $Schema> Task<$Timeline> create();
  }

}
