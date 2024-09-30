package gov.nasa.jpl.aerie.merlin.driver.retracing.engine.tracing;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

import static gov.nasa.jpl.aerie.merlin.driver.retracing.engine.tracing.Utilities.extractTask;

public class TaskTrace<T> {
  public List<Action<T>> actions = new ArrayList<>();
  public End<T> end;
  public MutableObject<Executor> executor;

  private TaskTrace(MutableObject<Executor> executor, final TaskResumptionInfo<T> info) {
    this.end = new End.Unfinished<>(info);
    this.executor = executor;
  }

  public static <T> TaskTrace<T> root(TaskFactory<T> rootTask) {
    return new TaskTrace<>(new MutableObject<>(null), new TaskResumptionInfo<>(new ArrayList<>(), new MutableInt(0), rootTask));
  }

  public void add(Action<T> entry) {
    this.actions.add(entry);
  }

  public void exit(End.Exit<T> exit) {
    if (!(this.end instanceof End.Unfinished)) throw new IllegalStateException();
    this.end = exit;
  }

  public <ReturnedType> TaskTrace<T> read(CellId<?> query, ReturnedType value) {
    if (!(this.end instanceof End.Unfinished<T> ending)) throw new IllegalStateException();
    TaskTrace<T> newTip;
    {
      final TaskTrace<T> res = new TaskTrace<>(executor, ending.info().duplicate());
      res.end = ending;
      newTip = res;
    }
    ending.info().reads().add(value);
    final var readRecords = new ArrayList<End.Read.Entry<T>>();
    readRecords.add(new End.Read.Entry<>(value, value.toString(), newTip));
    this.end = new End.Read<>(query, readRecords, ending.info().duplicate());
    return newTip;
  }

  TaskStatus<T> step(Scheduler scheduler, Cursor<T> cursor) {
    if (!(this.end instanceof End.Unfinished<T> unfinished)) throw new IllegalStateException();
    return unfinished.step(this, scheduler, cursor, unfinished.info(), this.executor.getValue());
  }

  public sealed interface End<T> {
    record Read<T>(CellId<?> query, List<Read.Entry<T>> entries, TaskResumptionInfo<T> info) implements
        End<T>
    {
      public record Entry<T>(Object value, String string, TaskTrace<T> rest) {}
      private <ReadValue> Optional<TaskTrace<T>> lookup(ReadValue readValue) {
        for (final var readRecord : entries()) {
          if (Objects.equals(readRecord.value(), readValue)) {
            return Optional.of(readRecord.rest);
          }
        }
        return Optional.empty();
      }
    }

    record Exit<T>(T returnValue) implements End<T> {}

    final class Unfinished<T> implements End<T> {
      private TraceWriter<T> writer;
      private Task<T> continuation;
      private final TaskResumptionInfo<T> info;

      public Unfinished(TaskResumptionInfo<T> info) {
        this.info = info;
      }

      TaskResumptionInfo<T> info() {
        return info;
      }

      boolean isActive() {
        if ((writer == null || continuation == null) && !(writer == null && continuation == null)) {
          throw new IllegalStateException();
        }
        return !(writer == null);
      }

      void init(TraceWriter<T> writer, Task<T> continuation) {
        this.writer = Objects.requireNonNull(writer);
        this.continuation = Objects.requireNonNull(continuation);
      }

      public TaskStatus<T> step(TaskTrace<T> trace, Scheduler scheduler, Cursor<T> cursor, TaskResumptionInfo<T> resumptionInfo, Executor executor) {
        if (!this.isActive()) {
          final var tr = new TaskRestarter<>(resumptionInfo.duplicate(), executor);
          final var writer = new TraceWriter<>(trace);
          final var status = tr.restart(writer.instrument(scheduler));
          writer.yield(status);

          this.init(writer, extractTask(status).orElse(null));
          cursor.trace = writer.trace;
          cursor.traceCounter = cursor.trace.actions.size();
          return status;
        } else {
          resumptionInfo.numSteps().increment();
          final var status = this.continuation.step(this.writer.instrument(scheduler));
          this.writer.yield(status);

          cursor.trace = this.writer.trace;
          cursor.traceCounter = cursor.trace.actions.size();

          return status;
        }
      }
    }
  }

  static <T> Cursor<T> cursor(TaskTrace<T> rbt) {
    return new Cursor<>(rbt);
  }

  public static class Cursor<T> {
    private TaskTrace<T> trace;
    private int traceCounter;

    public Cursor(TaskTrace<T> trace) {
      this.trace = trace;
    }

    public TaskStatus<T> step(Scheduler scheduler) {
      while (true) {
        List<Action<T>> actions = this.trace.actions;
        while (traceCounter < actions.size()) {
          final var action = actions.get(traceCounter);
          traceCounter++;
          switch (action) {
            case Action.Yield<T> a -> { return a.taskStatus(); }
            case Action.Emit<T, ?> a -> a.apply(scheduler);
            case Action.Spawn<T> a -> scheduler.spawn(a.childSpan(), a.child());
          }
        }

        switch (this.trace.end) {
          case End.Exit<T> e -> { return TaskStatus.completed(e.returnValue()); }
          case End.Unfinished<T> e -> {
            return this.trace.step(scheduler, this);
          }
          case End.Read<T> read -> {
            // Read the current value and use it to decide whether to continue down a trace, or start a new one
            final var readValue = scheduler.get(read.query());
            Optional<TaskTrace<T>> foundTrace = read.lookup(readValue);
            if (foundTrace.isPresent()) {
              this.trace = foundTrace.get();
              this.traceCounter = 0;
              continue;
            } else {
              final var rest = new TaskTrace<>(this.trace.executor, read.info().duplicate());
              read.entries().add(new End.Read.Entry<>(readValue, readValue.toString(), rest));
              return rest.step(scheduler, this);
            }
          }
        }
      }
    }
  }
}
