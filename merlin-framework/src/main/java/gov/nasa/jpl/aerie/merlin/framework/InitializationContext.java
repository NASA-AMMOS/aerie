package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class InitializationContext<$Schema> implements Context {
  private final Initializer<$Schema> builder;

  public InitializationContext(final Initializer<$Schema> builder) {
    this.builder = Objects.requireNonNull(builder);
  }

  public static <T> T initializing(final Initializer<?> builder, final Supplier<T> initializer) {
    return ModelActions.context.setWithin(new InitializationContext<>(builder), initializer::get);
  }

  @Override
  public ContextType getContextType() {
    return ContextType.Initializing;
  }

  @Override
  public <CellType> CellType ask(final Query<?, ?, CellType> query) {
    // SAFETY: All objects accessible within a single adaptation instance have the same brand.
    @SuppressWarnings("unchecked")
    final var brandedQuery = (Query<$Schema, ?, CellType>) query;

    return this.builder.getInitialState(brandedQuery);
  }

  @Override
  public <Event, Effect, CellType>
  Query<?, Event, CellType>
  allocate(final Projection<Event, Effect> projection, final Applicator<Effect, CellType> applicator) {
    return this.builder.allocate(projection, applicator);
  }

  @Override
  public <Event> void emit(final Event event, final Query<?, Event, ?> query) {
    throw new IllegalStateException("Cannot update simulation state during initialization");
  }

  @Override
  public String spawn(final TaskFactory task) {
    return this.builder.daemon(task::create);
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    throw new IllegalStateException("Cannot schedule activities during initialization");
  }

  @Override
  public String defer(final Duration duration, final TaskFactory task) {
    throw new IllegalStateException("Cannot schedule tasks during initialization");
  }

  @Override
  public String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
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
