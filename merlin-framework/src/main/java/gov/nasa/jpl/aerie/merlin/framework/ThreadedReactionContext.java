package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/* package-local */
final class ThreadedReactionContext implements Context {
  private final ExecutorService executor;
  private final Scoped<Context> rootContext;
  private final TaskHandle handle;
  private Scheduler scheduler;

  public ThreadedReactionContext(
      final ExecutorService executor,
      final Scoped<Context> rootContext,
      final Scheduler scheduler,
      final TaskHandle handle)
  {
    this.executor = Objects.requireNonNull(executor);
    this.rootContext = Objects.requireNonNull(rootContext);
    this.scheduler = scheduler;
    this.handle = handle;
  }

  @Override
  public ContextType getContextType() {
    return ContextType.Reacting;
  }

  @Override
  public <CellType> CellType ask(final Query<CellType> query) {
    return this.scheduler.get(query);
  }

  @Override
  public <Event, Effect, CellType> Query<CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> projection,
      final Topic<Event> topic)
  {
    throw new IllegalStateException("Cannot allocate during simulation");
  }

  @Override
  public <Event> void emit(final Event event, final Topic<Event> topic) {
    this.scheduler.emit(event, topic);
  }

  @Override
  public <T> String spawn(final TaskFactory<T> task) {
    return this.scheduler.spawn(task.create(this.executor));
  }

  @Override
  public <Output> String spawn(final Task<Output> task) {
    return this.scheduler.spawn(task);
  }

  @Override
  public void delay(final Duration duration) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.delay(duration);
  }

  @Override
  public void waitFor(final String id) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.await(id);
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
