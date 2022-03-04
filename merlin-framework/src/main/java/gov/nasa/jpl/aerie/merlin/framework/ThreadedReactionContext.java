package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/* package-local */
final class ThreadedReactionContext<Return> implements Context {
  private final ExecutorService executor;
  private final Scoped<Context> rootContext;
  private final TaskHandle<Return> handle;
  private Scheduler scheduler;

  public ThreadedReactionContext(
      final ExecutorService executor,
      final Scoped<Context> rootContext,
      final Scheduler scheduler,
      final TaskHandle<Return> handle)
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
  public <CellType> CellType ask(final Query<?, CellType> query) {
    return this.scheduler.get(query);
  }

  @Override
  public <Event, Effect, CellType> Query<Event, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> projection)
  {
    throw new IllegalStateException("Cannot allocate during simulation");
  }

  @Override
  public <Event> void emit(final Event event, final Query<Event, ?> query) {
    this.scheduler.emit(event, query);
  }

  @Override
  public <T> String spawn(final TaskFactory<T> task) {
    return this.scheduler.spawn(task.create(this.executor));
  }

  @Override
  public <Input, Output>
  String spawn(final DirectiveTypeId<Input, Output> id, final Input input, final Task<Output> task) {
    return this.scheduler.spawn(id, input, task);
  }

  @Override
  public void delay(final Duration duration) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.yield(TaskStatus.delayed(duration));
  }

  @Override
  public void waitFor(final String id) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.yield(TaskStatus.awaiting(id));
  }

  @Override
  public void waitUntil(final Condition condition) {
    this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
    this.scheduler = this.handle.yield(TaskStatus.awaiting((now, atLatest) -> {
      try (final var restore = this.rootContext.set(new QueryContext(now))) {
        return condition.nextSatisfied(true, Duration.ZERO, atLatest);
      }
    }));
  }
}
