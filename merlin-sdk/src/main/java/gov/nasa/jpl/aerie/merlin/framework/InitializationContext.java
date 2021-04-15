package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.Map;
import java.util.function.Supplier;

public final class InitializationContext implements Context {
  public static <T> T initializing(final Supplier<T> initializer) {
    return ModelActions.context.setWithin(new InitializationContext(), initializer::get);
  }

  @Override
  public <CellType> CellType ask(final Query<?, ?, CellType> query) {
    return query.getInitialValue();
  }

  @Override
  public <Event> void emit(final Event event, final Query<?, Event, ?> query) {
    throw new IllegalStateException("Cannot update simulation state during initialization");
  }

  @Override
  public String spawn(final Runnable task) {
    throw new IllegalStateException("Cannot schedule tasks during initialization");
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    throw new IllegalStateException("Cannot schedule activities during initialization");
  }

  @Override
  public String defer(final Duration duration, final Runnable task) {
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
