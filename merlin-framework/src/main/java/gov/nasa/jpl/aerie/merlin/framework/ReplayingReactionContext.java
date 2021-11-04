package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/* package-local */
final class ReplayingReactionContext<$Timeline> implements Context {
  private final ExecutorService executor;
  private final Scoped<Context> rootContext;
  private final TaskHandle<$Timeline> handle;
  private Scheduler<$Timeline> scheduler;

  private final MemoryCursor memory;

  public ReplayingReactionContext(
      final ExecutorService executor,
      final Scoped<Context> rootContext,
      final Memory memory,
      final Scheduler<$Timeline> scheduler,
      final TaskHandle<$Timeline> handle)
  {
    this.executor = Objects.requireNonNull(executor);
    this.rootContext = Objects.requireNonNull(rootContext);
    this.memory = new MemoryCursor(memory, new MutableInt(0), new MutableInt(0));
    this.scheduler = scheduler;
    this.handle = handle;
  }

  @Override
  public ContextType getContextType() {
    return ContextType.Reacting;
  }

  @Override
  public <CellType> CellType ask(final Query<?, ?, CellType> query) {
    return this.memory.doOnce(() -> {
      // SAFETY: All objects accessible within a single adaptation instance have the same brand.
      @SuppressWarnings("unchecked")
      final var brandedQuery = (Query<? super $Timeline, ?, CellType>) query;

      return this.scheduler.get(brandedQuery);
    });
  }

  @Override
  public <Event, Effect, CellType> Query<?, Event, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final Projection<Event, Effect> projection)
  {
    throw new IllegalStateException("Cannot allocate during simulation");
  }

  @Override
  public <Event> void emit(final Event event, final Query<?, Event, ?> query) {
    this.memory.doOnce(() -> {
      // SAFETY: All objects accessible within a single adaptation instance have the same brand.
      @SuppressWarnings("unchecked")
      final var brandedQuery = (Query<? super $Timeline, Event, ?>) query;

      this.scheduler.emit(event, brandedQuery);
    });
  }

  @Override
  public String spawn(final TaskFactory task) {
    return this.memory.doOnce(() -> {
      return this.scheduler.spawn(task.create(this.executor));
    });
  }

  @Override
  public String spawn(final String type, final Map<String, SerializedValue> arguments) {
    return this.memory.doOnce(() -> {
      return this.scheduler.spawn(type, arguments);
    });
  }

  @Override
  public String defer(final Duration duration, final TaskFactory task) {
    return this.memory.doOnce(() -> {
      return this.scheduler.defer(duration, task.create(this.executor));
    });
  }

  @Override
  public String defer(final Duration duration, final String type, final Map<String, SerializedValue> arguments) {
    return this.memory.doOnce(() -> {
      return this.scheduler.defer(duration, type, arguments);
    });
  }

  @Override
  public void delay(final Duration duration) {
    this.memory.doOnce(() -> {
      this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
      this.scheduler = this.handle.yield(TaskStatus.delayed(duration));
    });
  }

  @Override
  public void waitFor(final String id) {
    this.memory.doOnce(() -> {
      this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
      this.scheduler = this.handle.yield(TaskStatus.awaiting(id));
    });
  }

  @Override
  public void waitUntil(final Condition condition) {
    this.memory.doOnce(() -> {
      this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
      this.scheduler = this.handle.yield(TaskStatus.awaiting((now, atLatest) -> {
        try (final var restore = this.rootContext.set(new QueryContext<>(now))) {
          return condition.nextSatisfied(true, Duration.ZERO, atLatest);
        }
      }));
    });
  }

  public record Memory(List<Object> reads, MutableInt writes) {
    public void clear() {
      this.reads().clear();
      this.writes().setValue(0);
    }
  }

  private record MemoryCursor(Memory memory, MutableInt nextRead, MutableInt nextWrite) {
    public void doOnce(final Runnable action) {
      if (!hasCachedWrite()) {
        // Flag a write *before* we run, because we'll likely yield out via exception.
        this.memory.writes().add(1);
        action.run();
      }

      this.nextWrite.add(1);
    }

    public <T> T doOnce(final Supplier<T> action) {
      final T value;
      if (!hasCachedRead()) {
        value = action.get();
        this.memory.reads().add(value);
      } else {
        // SAFETY: Tasks are deterministic, and last time we did this action, we cached a T.
        @SuppressWarnings("unchecked")
        final var cachedValue = (T) this.memory.reads().get(this.nextRead.getValue());
        value = cachedValue;
      }

      this.nextRead.add(1);
      return value;
    }

    private boolean hasCachedRead() {
      return (this.nextRead.getValue() < this.memory.reads().size());
    }

    private boolean hasCachedWrite() {
      return (this.nextWrite.getValue() < this.memory.writes().getValue());
    }
  }
}
