package gov.nasa.jpl.aerie.merlin.protocol.model;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InSpan;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Task<Output> {
  /**
   * Perform one step of the task, returning the next step of the task and the conditions under which to perform it.
   *
   * <p>Clients must only call {@code step()} at most once, and must not invoke {@code step()} after {@link #release()}
   * has been invoked.</p>
   */
  TaskStatus<Output> step(Scheduler scheduler);

  /**
   * Release any transient system resources allocated to this task.
   *
   * <p>Any system resources held must be released by this method, so that garbage collection can take care of the rest.
   * For instance, if this object makes use of an OS-level Thread, that thread must be explicitly released to avoid
   * resource leaks</p>
   *
   * <p>This method <b>shall not</b> be called on this object after invoking {@code #step(Scheduler)};
   * nor shall {@link #step(Scheduler)} be called after this method.</p>
   */
  default void release() {}

  default <Output> Task<Output> andThen(Task<Output> task2) {
    return new Task<>() {
      @Override
      public TaskStatus<Output> step(final Scheduler scheduler) {
        switch (Task.this.step(scheduler)) {
          case TaskStatus.Completed<?> s -> {
            return task2.step(scheduler);
          }
          case TaskStatus.AwaitingCondition<?> s -> {
            return new TaskStatus.AwaitingCondition<>(s.condition(), s.continuation().andThen(task2));
          }
          case TaskStatus.CallingTask<?> s -> {
            return new TaskStatus.CallingTask<>(s.childSpan(), s.child(), s.continuation().andThen(task2));
          }
          case TaskStatus.Delayed<?> s -> {
            return new TaskStatus.Delayed<>(s.delay(), s.continuation().andThen(task2));
          }
        }
      }
    };
  }

  default Task<Unit> dropOutput() {
    return new Task<>() {
      @Override
      public TaskStatus<Unit> step(final Scheduler scheduler) {
        switch (this.step(scheduler)) {
          case TaskStatus.Completed<?> s -> {
            return TaskStatus.completed(Unit.UNIT);
          }
          case TaskStatus.AwaitingCondition<?> s -> {
            return new TaskStatus.AwaitingCondition<>(s.condition(), s.continuation().dropOutput());
          }
          case TaskStatus.CallingTask<?> s -> {
            return new TaskStatus.CallingTask<>(s.childSpan(), s.child(), s.continuation().dropOutput());
          }
          case TaskStatus.Delayed<?> s -> {
            return new TaskStatus.Delayed<>(s.delay(), s.continuation().dropOutput());
          }
        }
      }
    };
  }

  static <Output> Task<Unit> calling(Task<Output> task) {
    return new Task<Unit>() {
      @Override
      public TaskStatus<Unit> step(final Scheduler scheduler) {
        return TaskStatus.calling(InSpan.Parent, (TaskFactory < Output >)executor -> task, Task.empty());
      }
    };
  }

  static <Output> Task<Unit> callingWithSpan(Task<Output> task) {
    return new Task<Unit>() {
      @Override
      public TaskStatus<Unit> step(final Scheduler scheduler) {
        return TaskStatus.calling(InSpan.Fresh, (TaskFactory<Output>) executor -> task, Task.empty());
      }
    };
  }

  static Task<Unit> delaying(Duration duration) {
    return Task.of($ -> TaskStatus.delayed(duration, Task.empty()));
  }

  static <EventType> Task<Unit> emitting(EventType eventType, Topic<EventType> topic) {
    return Task.run($ -> $.emit(eventType, topic));
  }

  static Task<Unit> spawning(TaskFactory<?> taskFactory) {
    return Task.run($ -> $.spawn(InSpan.Parent, taskFactory));
  }

  static Task<Unit> spawning(Consumer<Scheduler> f) {
    return Task.run($ -> $.spawn(InSpan.Parent, (TaskFactory<Unit>) executor -> Task.run(f)));
  }

  static Task<Unit> spawningWithSpan(TaskFactory<?> taskFactory) {
    return Task.run($ -> $.spawn(InSpan.Fresh, taskFactory));
  }

  static Task<Unit> spawningWithSpan(Consumer<Scheduler> f) {
    return Task.run($ -> $.spawn(InSpan.Fresh, (TaskFactory<Unit>) executor -> Task.run(f)));
  }

  static <Any> Task<Unit> spawning(List<TaskFactory<Any>> tasks) {
    return Task.run($ -> {
      for (final var task : tasks) {
        $.spawn(InSpan.Fresh, task);
      }
    });
  }

  /**
   * @param f Must not yield
   * @return
   */
  static Task<Unit> run(Consumer<Scheduler> f) {
    return Task.evaluate(scheduler -> {
      f.accept(scheduler);
      return Unit.UNIT;
    });
  }

  static <Output> Task<Output> evaluate(Function<Scheduler, Output> f) {
    return new Task<>() {
      @Override
      public TaskStatus<Output> step(final Scheduler scheduler) {
        return TaskStatus.completed(f.apply(scheduler));
      }
    };
  }

  static Task<Unit> empty() {
    return new Task<>() {
      @Override
      public TaskStatus<Unit> step(final Scheduler scheduler) {
        return TaskStatus.completed(Unit.UNIT);
      }
    };
  }

  static <Output> Task<Output> of(Function<Scheduler, TaskStatus<Output>> f) {
    return new Task<Output>() {
      @Override
      public TaskStatus<Output> step(final Scheduler scheduler) {
        return f.apply(scheduler);
      }
    };
  }
}
