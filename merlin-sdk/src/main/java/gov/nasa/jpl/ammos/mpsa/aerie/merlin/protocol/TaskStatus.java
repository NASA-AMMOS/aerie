package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Solver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Objects;

public abstract class TaskStatus<$Timeline> {
  private TaskStatus() {}

  public abstract <Result> Result match(final Visitor<$Timeline, Result> visitor);

  public interface Visitor<$Timeline, Result> {
    Result completed();

    Result awaiting(String activityId);

    Result delayed(Duration delay);

    <DynamicsType, ConditionType>
    Result awaiting(
        Solver<?, DynamicsType, ConditionType> solver,
        Resource<? super $Timeline, DynamicsType> resource,
        ConditionType condition);
  }

  public static <$Timeline> TaskStatus<$Timeline> completed() {
    return new TaskStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.completed();
      }
    };
  }

  public static <$Timeline> TaskStatus<$Timeline> awaiting(final String id) {
    Objects.requireNonNull(id);

    return new TaskStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.awaiting(id);
      }
    };
  }

  public static <$Timeline> TaskStatus<$Timeline> delayed(final Duration delay) {
    Objects.requireNonNull(delay);

    return new TaskStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.delayed(delay);
      }
    };
  }

  public static <$Timeline, DynamicsType, ConditionType>
  TaskStatus<$Timeline>
  awaiting(
      final Solver<?, DynamicsType, ConditionType> solver,
      final Resource<? super $Timeline, DynamicsType> resource,
      final ConditionType condition)
  {
    Objects.requireNonNull(solver);
    Objects.requireNonNull(resource);
    Objects.requireNonNull(condition);

    return new TaskStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.awaiting(solver, resource, condition);
      }
    };
  }
}
