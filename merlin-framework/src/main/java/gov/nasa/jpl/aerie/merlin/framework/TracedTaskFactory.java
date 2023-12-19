package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

public class TracedTaskFactory<T> implements TaskFactory<T> {
  private final TaskFactory<T> taskFactory;
  public static List<Pair<Task.Key, TaskTrace.ModifiableOnce<TaskTrace>>> taskTraces = new ArrayList<>();

  public TracedTaskFactory(TaskFactory<T> taskFactory) {
    this.taskFactory = taskFactory;
  }

  public static <T> TaskFactory<T> trace(TaskFactory<T> taskFactory) {
    return new TracedTaskFactory<>(taskFactory);
  }

  @Override
  public String toString() {
    return taskFactory.toString();
  }

  @Override
  public Task<T> create(final Executor executor) {
    final var task = taskFactory.create(executor);
    final Task.Key taskKey = task.getKey();
    Optional<TaskTrace> trace = lookupTaskTrace(taskKey);
    if (trace.isPresent()) {
      System.out.println("Replaying a task using a cursor!");
      final var cursor = TaskTrace.cursor(trace.get());
      return new Task<T>() {
        @Override
        public TaskStatus<T> step(final Scheduler scheduler) {
          while (true) {
            final var nextAction = cursor.nextAction();
            System.out.println(nextAction);
            if (nextAction instanceof Action.Yield a) {
              return (TaskStatus<T>) a.taskStatus();
            } else if (nextAction instanceof Action.Emit<?> a) {
              emit(scheduler, a);
            } else if (nextAction instanceof Action.Spawn a) {
              throw new NotImplementedException();
            } else if (nextAction instanceof Action.Read a) {
              final var result = scheduler.get(a.query());
              final var restartInfo = cursor.read(result);
              if (restartInfo.isEmpty()) {
                continue;
              } else {
                throw new NotImplementedException();
              }
            }
          }
        }
      };
    } else {
      final var writer = TaskTrace.writer();
      taskTraces.add(Pair.of(taskKey, writer.trace));
      return new Task<>() {
        private Task<T> continuation = task;

        @Override
        public TaskStatus<T> step(final Scheduler scheduler) {
          final var taskStatus = continuation.step(new Scheduler() {
            @Override
            public <State> State get(final CellId<State> cellId) {
              final State state = scheduler.get(cellId);
              writer.read(cellId, state);
              return state;
            }

            @Override
            public <Event> void emit(final Event event, final Topic<Event> topic) {
              writer.emit(event, topic);
              scheduler.emit(event, topic);
            }

            @Override
            public void spawn(final TaskFactory<?> task1) {
              writer.spawn();
              scheduler.spawn(task1);
            }
          });
          writer.yield(taskStatus);
          if (taskStatus instanceof TaskStatus.Completed<T> s) {
            return s;
          } else if (taskStatus instanceof TaskStatus.Delayed<T> s) {
            this.continuation = s.continuation();
            return new TaskStatus.Delayed<>(s.delay(), this);
          } else if (taskStatus instanceof TaskStatus.CallingTask<T> s) {
            this.continuation = s.continuation();
            return new TaskStatus.CallingTask<>(s.child(), this);
          } else if (taskStatus instanceof TaskStatus.AwaitingCondition<T> s) {
            this.continuation = s.continuation();
            return new TaskStatus.AwaitingCondition<>(s.condition(), this);
          } else {
            throw new Error("Unhandled variant of TaskStatus: " + taskStatus);
          }
        }
      };
    }
  }

  private static <T> void emit(Scheduler scheduler, Action.Emit<T> a) {
    scheduler.emit(a.event(), a.topic());
  }

  private static Optional<TaskTrace> lookupTaskTrace(Task.Key key) {
    for (final var taskTrace : taskTraces) {
      if (taskTrace.getLeft().match(key)) {
        return Optional.of(taskTrace.getRight().get());
      }
    }
    return Optional.empty();
  }
}
