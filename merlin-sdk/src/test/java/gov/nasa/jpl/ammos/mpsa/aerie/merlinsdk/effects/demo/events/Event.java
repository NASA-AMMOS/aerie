package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events;

import java.util.Objects;

// This can be mechanically derived from `EventHandler`.
public abstract class Event {
  private Event() {}

  public abstract <Result> Result visit(EventHandler<Result> visitor);

  public static Event addDataRate(final String binName, final double amount) {
    Objects.requireNonNull(binName);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.addDataRate(binName, amount);
      }
    };
  }

  public static Event clearDataRate(final String binName) {
    Objects.requireNonNull(binName);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.clearDataRate(binName);
      }
    };
  }

  public static Event log(final String message) {
    Objects.requireNonNull(message);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.log(message);
      }
    };
  }

  public static Event instantiateActivity(final String activityId, final String activityName) {
    Objects.requireNonNull(activityId);
    Objects.requireNonNull(activityName);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.instantiateActivity(activityId, activityName);
      }
    };
  }

  public static Event resumeActivity(final String activityId) {
    Objects.requireNonNull(activityId);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.resumeActivity(activityId);
      }
    };
  }

  @Override
  public final String toString() {
    return this.visit(new EventHandler<>() {
      @Override
      public String addDataRate(final String binName, final double amount) {
        return String.format("addDataRate(\"%s\", %s)",
            binName.replace("\\", "\\\\").replace("\"", "\\\""),
            amount);
      }

      @Override
      public String clearDataRate(final String binName) {
        return String.format("clearDataRate(\"%s\")",
            binName.replace("\\", "\\\\").replace("\"", "\\\""));
      }

      @Override
      public String log(final String message) {
        return String.format("log(\"%s\")",
            message.replace("\\", "\\\\").replace("\"", "\\\""));
      }

      @Override
      public String instantiateActivity(final String activityId, final String activityType) {
        return String.format("instantiateActivity(\"%s\", \"%s\")",
            activityId.replace("\\", "\\\\").replace("\"", "\\\""),
            activityType.replace("\\", "\\\\").replace("\"", "\\\""));
      }

      @Override
      public String resumeActivity(final String activityId) {
        return String.format("resumeActivity(\"%s\")",
            activityId.replace("\\", "\\\\").replace("\"", "\\\""));
      }
    });
  }
}
