package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.Function;

public interface Context {
  enum ContextType { Initializing, Reacting, Querying }

  // Usable in all contexts
  ContextType getContextType();

  // Usable during both initialization & simulation
  <State> State ask(CellId<State> cellId);

  // Usable during initialization
  <Event, Effect, State>
  CellId<State>
  allocate(
      State initialState,
      CellType<Effect, State> cellType,
      Function<Event, Effect> interpretation,
      Topic<Event> topic);

  // Usable during simulation
  <Event> void emit(Event event, Topic<Event> topic);

  <Input> void spawn(TaskFactory<Input, ?> task, Input input);
  <Input, Output> Output call(TaskFactory<Input, Output> task, Input input);

  void delay(Duration duration);
  void waitUntil(Condition condition);
}
