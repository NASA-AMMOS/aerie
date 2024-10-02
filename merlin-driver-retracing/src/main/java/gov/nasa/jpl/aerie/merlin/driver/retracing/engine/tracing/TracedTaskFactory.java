package gov.nasa.jpl.aerie.merlin.driver.retracing.engine.tracing;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.concurrent.Executor;

public class TracedTaskFactory<T> implements TaskFactory<T> {
  private final TaskTrace<T> trace ;

  public TracedTaskFactory(TaskFactory<T> taskFactory) {
    this.trace = TaskTrace.root(taskFactory);
  }

  public static <T> TaskFactory<T> trace(TaskFactory<T> taskFactory) {
    if (taskFactory instanceof TracedTaskFactory<T>) {
      return taskFactory;
    } else {
      return new TracedTaskFactory<>(taskFactory);
    }
  }

  @Override
  public Task<T> create(final Executor executor) {
    return new ImitatingTask<>(trace, executor);
  }

  static class ImitatingTask<Output> implements Task<Output> {
    private final TaskTrace.Cursor<Output> cursor;

    public ImitatingTask(TaskTrace<Output> taskTrace, Executor executor) {
      this.cursor = TaskTrace.cursor(taskTrace);
      taskTrace.executor.setValue(executor);
    }

    @Override
    public TaskStatus<Output> step(Scheduler scheduler) {
      return withContinuation(cursor.step(new Scheduler() {
        @Override
        public <State> State get(final CellId<State> cellId) {
          return scheduler.get(cellId);
        }

        @Override
        public <Event> void emit(final Event event, final Topic<Event> topic) {
            scheduler.emit(event, topic);
        }

        @Override
        public void spawn(final InSpan taskSpan, final TaskFactory<?> task) {
          scheduler.spawn(taskSpan, task);
        }
      }));
    }

    @Override
    public void release() {
      // TODO
    }

    private TaskStatus<Output> replaceContinuation(TaskStatus<Output> taskStatus) {
      switch (taskStatus) {
        case TaskStatus.Completed<Output> s -> {
          return s;
        }
        case TaskStatus.Delayed<Output> s -> {
          return new TaskStatus.Delayed<>(s.delay(), this);
        }
        case TaskStatus.CallingTask<Output> s -> {
          return new TaskStatus.CallingTask<>(s.childSpan(), s.child(), this);
        }
        case TaskStatus.AwaitingCondition<Output> s -> {
          return new TaskStatus.AwaitingCondition<>(s.condition(), this);
        }
      }
    }

    private TaskStatus<Output> withContinuation(Action.Status<Output> status) {
      switch (status) {
        case Action.Status.Completed<Output> s -> {
          return new TaskStatus.Completed<>(s.returnValue());
        }
        case Action.Status.Delayed<Output> s -> {
          return new TaskStatus.Delayed<>(s.delay(), this);
        }
        case Action.Status.CallingTask<Output> s -> {
          return new TaskStatus.CallingTask<>(s.childSpan(), s.child(), this);
        }
        case Action.Status.AwaitingCondition<Output> s -> {
          return new TaskStatus.AwaitingCondition<>(s.condition(), this);
        }
      }
    }
  }
}
