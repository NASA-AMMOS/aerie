package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.function.Function;

/* package-local */
final class ThreadedReactionContext implements Context {
  private final Scoped<Context> rootContext;
  private final TaskHandle handle;
  private Scheduler scheduler;

  public ThreadedReactionContext(
      final Scoped<Context> rootContext,
      final Scheduler scheduler,
      final TaskHandle handle)
  {
    this.rootContext = Objects.requireNonNull(rootContext);
    this.scheduler = scheduler;
    this.handle = handle;
  }

  @Override
  public ContextType getContextType() {
    return ContextType.Reacting;
  }

  @Override
  public <State> State ask(final CellId<State> cellId) {
    return this.scheduler.get(cellId);
  }

  @Override
  public <Event, Effect, State>
  CellId<State> allocate(
      final State initialState,
      final CellType<Effect, State> cellType,
      final Function<Event, Effect> interpretation,
      final Topic<Event> topic)
  {
    throw new IllegalStateException("Cannot allocate during simulation");
  }

  @Override
  public <Event> void emit(final Event event, final Topic<Event> topic) {
    this.scheduler.emit(event, topic);
  }

  @Override
  public <T> void startActivity(final T activity, final Topic<T> inputTopic) {
    this.scheduler.startActivity(activity, inputTopic);
  }

  @Override
  public <T> void endActivity(final T result, final Topic<T> outputTopic) {
    this.scheduler.endActivity(result, outputTopic);
  }

  @Override
  public void spawn(final TaskFactory<?> task) {
    this.scheduler.spawn(task);
  }

  @Override
  public <T> void call(final TaskFactory<T> task) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.call(task);
  }

  @Override
  public void pushSpan() {
    this.scheduler.pushSpan();
  }

  @Override
  public void popSpan() {
    this.scheduler.popSpan();
  }

  @Override
  public void delay(final Duration duration) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.delay(duration);
  }

  @Override
  public void waitUntil(final Condition condition) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.await((now, atLatest) -> {
      try (final var restore = this.rootContext.set(new QueryContext(now))) {
        return condition.nextSatisfied(true, Duration.ZERO, atLatest);
      }
    });
  }
}
