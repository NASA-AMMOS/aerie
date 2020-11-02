package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Objects;

public abstract class ActivityStatus {
  private ActivityStatus() {}

  public abstract <$Result> $Result match(final Visitor<$Result> visitor);

  public interface Visitor<$Result> {
    $Result completed();
    $Result awaiting(String id);
    $Result delayed(Duration delay);
  }

  public static ActivityStatus completed() {
    return new ActivityStatus() {
      @Override
      public <$Result> $Result match(final Visitor<$Result> visitor) {
        return visitor.completed();
      }
    };
  }

  public static ActivityStatus awaiting(final String id) {
    Objects.requireNonNull(id);

    return new ActivityStatus() {
      @Override
      public <$Result> $Result match(final Visitor<$Result> visitor) {
        return visitor.awaiting(id);
      }
    };
  }

  public static ActivityStatus delayed(final Duration delay) {
    Objects.requireNonNull(delay);

    return new ActivityStatus() {
      @Override
      public <$Result> $Result match(final Visitor<$Result> visitor) {
        return visitor.delayed(delay);
      }
    };
  }

  public static ActivityStatus delayed(final long quantity, final Duration unit) {
    return delayed(Duration.of(quantity, unit));
  }
}
