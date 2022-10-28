package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

public final class InitializationContext implements Context {
  private final ExecutorService executor;
  private final Initializer builder;

  public InitializationContext(final ExecutorService executor, final Initializer builder) {
    this.executor = Objects.requireNonNull(executor);
    this.builder = Objects.requireNonNull(builder);
  }

  public static <T>
  T initializing(final ExecutorService executor, final Initializer builder, final Supplier<T> initializer) {
    try (final var restore = ModelActions.context.set(new InitializationContext(executor, builder))) {
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
  public void spawn(final TaskFactory<?> task) {
    this.builder.daemon(wrap(task));
  }

  // It looks stupid, but this method is actually necessary to bind the type parameter T.
  // Inlining this method at its call site induces a compiler error.
  private <T> Initializer.TaskFactory<T> wrap(final TaskFactory<T> task) {
    return () -> task.create(this.executor);
  }

  @Override
  public <Return> void call(final TaskFactory<Return> task) {
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
