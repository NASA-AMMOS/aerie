package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.time.Duration;

import java.util.Objects;

public abstract class TaskStatus<$Timeline> {
  private TaskStatus() {}

  public abstract <Result> Result match(final Visitor<$Timeline, Result> visitor);

  public interface Visitor<$Timeline, Result> {
    Result completed();

    Result delayed(Duration delay);

    Result awaiting(String activityId);

    Result awaiting(Condition<? super $Timeline> condition);
  }

  public static <$Timeline> TaskStatus<$Timeline> completed() {
    return new TaskStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.completed();
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

  public static <$Timeline> TaskStatus<$Timeline> awaiting(final String id) {
    Objects.requireNonNull(id);

    return new TaskStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.awaiting(id);
      }
    };
  }

  public static <$Timeline> TaskStatus<$Timeline> awaiting(final Condition<? super $Timeline> condition) {
    Objects.requireNonNull(condition);

    return new TaskStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.awaiting(condition);
      }
    };
  }
}
