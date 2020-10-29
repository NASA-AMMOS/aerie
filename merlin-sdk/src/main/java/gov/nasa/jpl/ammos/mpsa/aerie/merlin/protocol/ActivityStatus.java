package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Objects;

public abstract class ActivityStatus<$ActivityId> {
  private ActivityStatus() {}

  public abstract <$Result> $Result match(final Visitor<$ActivityId, $Result> visitor);

  public interface Visitor<$ActivityId, $Result> {
    $Result completed();
    $Result awaiting($ActivityId id);
    $Result delayed(Duration delay);
  }

  public static <$ActivityId> ActivityStatus<$ActivityId> completed() {
    return new ActivityStatus<>() {
      @Override
      public <$Result> $Result match(final Visitor<$ActivityId, $Result> visitor) {
        return visitor.completed();
      }
    };
  }

  public static <$ActivityId> ActivityStatus<$ActivityId> awaiting(final $ActivityId id) {
    Objects.requireNonNull(id);

    return new ActivityStatus<>() {
      @Override
      public <$Result> $Result match(final Visitor<$ActivityId, $Result> visitor) {
        return visitor.awaiting(id);
      }
    };
  }

  public static <$ActivityId> ActivityStatus<$ActivityId> delayed(final Duration delay) {
    Objects.requireNonNull(delay);

    return new ActivityStatus<>() {
      @Override
      public <$Result> $Result match(final Visitor<$ActivityId, $Result> visitor) {
        return visitor.delayed(delay);
      }
    };
  }

  public static <$ActivityId> ActivityStatus<$ActivityId> delayed(final long quantity, final Duration unit) {
    return delayed(Duration.of(quantity, unit));
  }
}
