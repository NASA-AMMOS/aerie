package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
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
  public <CellType> CellType ask(final Query<?, CellType> query) {
    return this.builder.getInitialState(query);
  }

  @Override
  public <Event, Effect, CellType>
  Query<Event, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> projection
  ) {
    return this.builder.allocate(initialState, applicator, trait, projection);
  }

  @Override
  public <Event> void emit(final Event event, final Query<Event, ?> query) {
    throw new IllegalStateException("Cannot update simulation state during initialization");
  }

  @Override
  public <Return> String spawn(final TaskFactory<Return> task) {
    return this.builder.daemon(() -> task.create(InitializationContext.this.executor));
  }

  @Override
  public <Input, Output>
  String spawn(final DirectiveTypeId<Input, Output> id, final Input activity, final Task<Output> task) {
    throw new IllegalStateException("Cannot schedule activities during initialization");
  }

  @Override
  public void delay(final Duration duration) {
    throw new IllegalStateException("Cannot yield during initialization");
  }

  @Override
  public void waitFor(final String id) {
    throw new IllegalStateException("Cannot yield during initialization");
  }

  @Override
  public void waitUntil(final Condition condition) {
    throw new IllegalStateException("Cannot yield during initialization");
  }
}
