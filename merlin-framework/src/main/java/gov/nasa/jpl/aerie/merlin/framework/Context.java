package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public interface Context {
  enum ContextType { Initializing, Reacting, Querying }

  // Usable in all contexts
  ContextType getContextType();

  // Usable during both initialization & simulation
  <CellType> CellType ask(Query<?, CellType> query);

  // Usable during initialization
  <Event, Effect, CellType>
  Query<Event, CellType>
  allocate(
      CellType initialState,
      Applicator<Effect, CellType> applicator,
      EffectTrait<Effect> trait,
      Function<Event, Effect> projection);

  // Usable during simulation
  <Event> void emit(Event event, Query<Event, ?> query);

  interface TaskFactory<Return> { Task<Return> create(ExecutorService executor); }

  <Return> String spawn(TaskFactory<Return> task);
  <Input, Output> String spawn(DirectiveTypeId<Input, Output> id, Input activity, Task<Output> task);

  void delay(Duration duration);
  void waitFor(String id);
  void waitUntil(Condition condition);
}
