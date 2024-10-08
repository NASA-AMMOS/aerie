package gov.nasa.jpl.aerie.merlin.driver.retracing.tracing;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.Optional;

public class Utilities {
  public static <T> Optional<Task<T>> extractTask(TaskStatus<T> status) {
    return switch (status) {
      case TaskStatus.AwaitingCondition<T> v -> Optional.of(v.continuation());
      case TaskStatus.CallingTask<T> v -> Optional.of(v.continuation());
      case TaskStatus.Completed<T> v -> Optional.empty();
      case TaskStatus.Delayed<T> v -> Optional.of(v.continuation());
    };
  }
}
