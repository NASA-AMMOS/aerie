package gov.nasa.jpl.aerie.merlin.driver.retracing.tracing;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
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
      return withContinuation(cursor.step(scheduler));
    }

    @Override
    public void release() {
      // TODO
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
