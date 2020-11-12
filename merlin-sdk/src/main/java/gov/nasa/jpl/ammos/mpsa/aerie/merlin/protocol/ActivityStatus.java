package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Objects;

public abstract class ActivityStatus<$Timeline> {
  private ActivityStatus() {}

  public abstract <Result> Result match(final Visitor<$Timeline, Result> visitor);

  public interface Visitor<$Timeline, Result> {
    Result completed();

    Result awaiting(String activityId);

    Result delayed(Duration delay);

    <ResourceType, ConditionType>
    Result awaiting(
        Resource<History<$Timeline, ?>, SolvableDynamics<ResourceType, ConditionType>> resource,
        ConditionType condition);
  }

  public static <$Timeline> ActivityStatus<$Timeline> completed() {
    return new ActivityStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.completed();
      }
    };
  }

  public static <$Timeline> ActivityStatus<$Timeline> awaiting(final String id) {
    Objects.requireNonNull(id);

    return new ActivityStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.awaiting(id);
      }
    };
  }

  public static <$Timeline> ActivityStatus<$Timeline> delayed(final Duration delay) {
    Objects.requireNonNull(delay);

    return new ActivityStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.delayed(delay);
      }
    };
  }

  public static <$Timeline, ResourceType, ConditionType>
  ActivityStatus<$Timeline>
  awaiting(
      final Resource<History<$Timeline, ?>, SolvableDynamics<ResourceType, ConditionType>> resource,
      final ConditionType condition)
  {
    Objects.requireNonNull(resource);
    Objects.requireNonNull(condition);

    return new ActivityStatus<>() {
      @Override
      public <Result> Result match(final Visitor<$Timeline, Result> visitor) {
        return visitor.awaiting(resource, condition);
      }
    };
  }

  public static <$Timeline> ActivityStatus<$Timeline> delayed(final long quantity, final Duration unit) {
    return delayed(Duration.of(quantity, unit));
  }
}
