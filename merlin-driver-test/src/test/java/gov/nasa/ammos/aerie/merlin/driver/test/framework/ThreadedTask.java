package gov.nasa.ammos.aerie.merlin.driver.test.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public record ThreadedTask<T>(TestRegistrar.CellMap cellMap, Supplier<T> task, TaskThread<T> thread, MutableBoolean finished) implements Task<T> {
  public static <T> ThreadedTask<T> of(Executor executor, TestRegistrar.CellMap cellMap, Supplier<T> task) {
    return new ThreadedTask<>(cellMap, task, TaskThread.start(executor, task), new MutableBoolean(false));
  }

  @Override
  public TaskStatus<T> step(final Scheduler scheduler) {
    if (finished.getValue()) {
      throw new IllegalStateException("Stepping finished task");
    }
    TestContext.set(new TestContext.Context(cellMap, scheduler, this));
    try {
      thread.inbox().put(new Message.Resume());
      final var response = thread.outbox().take();
      if (response instanceof ThreadedTaskStatus.Aborted<T> r) {
        throw new RuntimeException(r.throwable());
      }
      if (response instanceof ThreadedTaskStatus.Completed<T> r) {
        finished.setTrue();
      }
      return response.withContinuation(this);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      TestContext.clear();
    }
  }

  @Override
  public void release() {
    try {
      thread.inbox.put(new Message.Abort());
    } catch (final InterruptedException ex) {
      return;
    }
  }

  sealed interface Message {
    record Resume() implements Message {}

    record Abort() implements Message {}
  }

  record TaskThread<T>(
      Supplier<T> task,
      ArrayBlockingQueue<Message> inbox,
      ArrayBlockingQueue<ThreadedTaskStatus<T>> outbox
  )
  {
    public static <T> TaskThread<T> start(Executor executor, Supplier<T> task) {
      final var taskThread = new TaskThread<>(
          task,
          new ArrayBlockingQueue<>(1),
          new ArrayBlockingQueue<>(1));
      executor.execute(taskThread::start);
      return taskThread;
    }

    private void start() {
      try {
        if (inbox.take() instanceof Message.Abort) outbox.put(null);
        outbox.put(new ThreadedTaskStatus.Completed<>(task.get()));
      } catch (InterruptedException e) {
        return; //throw new RuntimeException(e);
      } catch (Throwable throwable) {
        try {
          outbox.put(new ThreadedTaskStatus.Aborted<>(throwable));
        } catch (InterruptedException e) {
          return; //throw new RuntimeException(e);
        }
      }
    }

    void delay(Duration duration) {
      try {
        outbox.put(new ThreadedTaskStatus.Delayed<>(duration));
        inbox.take();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    void call(InSpan childSpan, TaskFactory<?> child) {
      try {
        outbox.put(new ThreadedTaskStatus.CallingTask<>(childSpan, child));
        inbox.take();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    void waitUntil(Condition condition) {
      try {
        outbox.put(new ThreadedTaskStatus.AwaitingCondition<>(condition));
        inbox.take();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public sealed interface ThreadedTaskStatus<Return> {
    record Completed<Return>(Return returnValue) implements ThreadedTaskStatus<Return> {}

    record Delayed<Return>(Duration delay) implements ThreadedTaskStatus<Return> {}

    record CallingTask<Return>(InSpan childSpan, TaskFactory<?> child) implements ThreadedTaskStatus<Return> {}

    record AwaitingCondition<Return>(Condition condition) implements ThreadedTaskStatus<Return> {}

    record Aborted<Return>(Throwable throwable) implements ThreadedTaskStatus<Return> {}

    default TaskStatus<Return> withContinuation(Task<Return> continuation) {
      return ThreadedTask.withContinuation(this, continuation);
    }
  }

  private static <T> TaskStatus<T> withContinuation(ThreadedTaskStatus<T> take, Task<T> continuation) {
    return switch (take) {
      case ThreadedTaskStatus.AwaitingCondition<T> s -> TaskStatus.awaiting(s.condition(), continuation);
      case ThreadedTaskStatus.CallingTask<T> s -> TaskStatus.calling(s.childSpan(), s.child(), continuation);
      case ThreadedTaskStatus.Completed<T> s -> TaskStatus.completed(s.returnValue());
      case ThreadedTaskStatus.Delayed<T> s -> TaskStatus.delayed(s.delay, continuation);
      case ThreadedTaskStatus.Aborted<T> s -> throw new RuntimeException(s.throwable());
    };
  }
}
