package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.time.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SimulationKernel {
  // A time-ordered queue of all scheduled future tasks.
  public final TaskQueue<String> queue = new TaskQueue<>();
  // The set of all tasks that have completed.
  private final Set<String> completedTasks = new HashSet<>();
  // For each task, a set of tasks awaiting its completion.
  private final Map<String, Set<String>> waitingTasks = new HashMap<>();


  public Duration getElapsedTime() {
    return this.queue.getElapsedTime();
  }

  public interface Executor<$Timeline> {
    History<$Timeline> execute(Duration delta, TaskFrame<$Timeline, String> frame);
  }

  public <$Timeline> History<$Timeline>
  consumeUpTo(final Duration maximum, final History<$Timeline> startTime, final Executor<$Timeline> executor) {
    var elapsedTime = this.queue.getElapsedTime();
    var now = startTime;
    var frame = this.queue.popNextFrame(now, maximum);

    while (frame.isPresent()) {
      final var nextTime = this.queue.getElapsedTime();
      final var delta = nextTime.minus(elapsedTime);

      elapsedTime = nextTime;
      now = executor.execute(delta, frame.get());
      frame = this.queue.popNextFrame(now, maximum);
    }

    return now;
  }

  public void complete(final String taskId) {
    this.completedTasks.add(taskId);

    final var conditionedActivities = this.waitingTasks.remove(taskId);
    if (conditionedActivities != null) {
      for (final var conditionedTask : conditionedActivities) {
        this.queue.deferTo(this.queue.getElapsedTime(), conditionedTask);
      }
    }
  }

  public void delay(final String taskId, final Duration delay) {
    this.queue.deferTo(this.queue.getElapsedTime().plus(delay), taskId);
  }

  public void awaitCompletion(final String taskId, final String target) {
    if (this.completedTasks.contains(target)) {
      this.queue.deferTo(this.queue.getElapsedTime(), taskId);
    } else {
      this.waitingTasks.computeIfAbsent(target, k -> new HashSet<>()).add(taskId);
    }
  }

  public <$Timeline>
  void updateByStatus(final String taskId, final TaskStatus<$Timeline> status) {
    /*
    In a reasonable world (i.e. JDK 16 + sealed classes), we could write this instead:
      switch (status) {
        case Completed ->
          complete(info);
        case Delayed(var delay) ->
          delay(info, delay);
        case AwaitingCompletion(var target) ->
          awaitCompletion(info, target);
        case AwaitingCondition(var condition) ->
          awaitCondition(info, condition);
      }
    Alas, this (JDK 11) is not a reasonable world.
    */
    status.match(new TaskStatus.Visitor<$Timeline, Void>() {
      @Override
      public Void completed() {
        complete(taskId);
        return null;
      }

      @Override
      public Void delayed(final Duration delay) {
        delay(taskId, delay);
        return null;
      }

      @Override
      public Void awaiting(final String target) {
        awaitCompletion(taskId, target);
        return null;
      }

      @Override
      public Void awaiting(final Condition<? super $Timeline> condition) {
        // TODO
        delay(taskId, Duration.ZERO);
        return null;
      }
    });
  }
}
