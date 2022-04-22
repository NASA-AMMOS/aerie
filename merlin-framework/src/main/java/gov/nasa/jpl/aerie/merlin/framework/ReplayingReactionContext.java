package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.DirectiveTypeId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

/* package-local */
final class ReplayingReactionContext implements Context {
  private final ExecutorService executor;
  private final Scoped<Context> rootContext;
  private final TaskHandle handle;
  private Scheduler scheduler;

  private final MemoryCursor memory;

  public ReplayingReactionContext(
      final ExecutorService executor,
      final Scoped<Context> rootContext,
      final Memory memory,
      final Scheduler scheduler,
      final TaskHandle handle)
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
  public <CellType> CellType ask(final Query<?, CellType> query) {
    return this.memory.doOnce(() -> {
      return this.scheduler.get(query);
    });
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
    this.memory.doOnce(() -> {
      this.scheduler.emit(event, query);
    });
  }

  @Override
  public <T> String spawn(final TaskFactory<T> task) {
    return this.memory.doOnce(() -> {
      return this.scheduler.spawn(task.create(this.executor));
    });
  }

  @Override
  public <Input, Output>
  String spawn(final DirectiveTypeId<Input, Output> id, final Input input, final Task<Output> task) {
    return this.memory.doOnce(() -> {
      return this.scheduler.spawn(id, input, task);
    });
  }

  @Override
  public void delay(final Duration duration) {
    this.memory.doOnce(() -> {
      this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
      this.scheduler = this.handle.delay(duration);
    });
  }

  @Override
  public void waitFor(final String id) {
    this.memory.doOnce(() -> {
      this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
      this.scheduler = this.handle.await(id);
    });
  }

  @Override
  public void waitUntil(final Condition condition) {
    this.memory.doOnce(() -> {
      this.scheduler = null;  // Relinquish the current scheduler before yielding, in case an exception is thrown.
      this.scheduler = this.handle.await((now, atLatest) -> {
        try (final var restore = this.rootContext.set(new QueryContext(now))) {
          return condition.nextSatisfied(true, Duration.ZERO, atLatest);
        }
      });
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
