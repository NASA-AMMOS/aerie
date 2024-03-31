package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.function.Function;

public interface Context {
  <T> void startActivity(T activity, Topic<T> inputTopic);
  <T> void endActivity(T result, Topic<T> outputTopic);
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

  void spawn(TaskFactory<?> task);
  default void spawn(String taskName, TaskFactory<?> task) {
    spawn(task);
  }

  <Return> void call(TaskFactory<Return> task);
  void pushSpan();
  void popSpan();

  void delay(Duration duration);
  void waitUntil(Condition condition);
}
