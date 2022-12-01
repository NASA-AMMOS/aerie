package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class InitializationContext implements Context {
  private final Initializer builder;

  public InitializationContext(final Initializer builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  public static <T>
  T initializing(final Initializer builder, final Supplier<T> initializer) {
    try (final var restore = ModelActions.context.set(new InitializationContext(builder))) {
      return initializer.get();
    }
  }

  @Override
  public ContextType getContextType() {
    return ContextType.Initializing;
  }

  @Override
  public <State> State ask(final CellId<State> cellId) {
    return this.builder.getInitialState(cellId);
  }

  @Override
  public <Event, Effect, State>
  CellId<State> allocate(
      final State initialState,
      final CellType<Effect, State> cellType,
      final Function<Event, Effect> interpretation,
      final Topic<Event> topic
  ) {
    return this.builder.allocate(initialState, cellType, interpretation, topic);
  }

  @Override
  public <Event> void emit(final Event event, final Topic<Event> topic) {
    throw new IllegalStateException("Cannot update simulation state during initialization");
  }

  @Override
  public <Input> void spawn(final TaskFactory<Input, ?> task, final Input input) {
    this.builder.daemon(task.butFirst(Task.lift($ -> input)));
  }

  @Override
  public <Input, Output> Output call(final TaskFactory<Input, Output> task, final Input input) {
    throw new IllegalStateException("Cannot yield during initialization");
  }

  @Override
  public void delay(final Duration duration) {
    throw new IllegalStateException("Cannot yield during initialization");
  }

  @Override
  public void waitUntil(final Condition condition) {
    throw new IllegalStateException("Cannot yield during initialization");
  }
}
