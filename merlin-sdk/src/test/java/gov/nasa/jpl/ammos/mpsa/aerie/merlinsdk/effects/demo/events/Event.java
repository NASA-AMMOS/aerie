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
        return visitor.dataAdded(binName, amount);
      }
    };
  }

  public static Event clearBin(final String binName) {
    Objects.requireNonNull(binName);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.binCleared(binName);
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

  public static Event run(final String activityName) {
    Objects.requireNonNull(activityName);
    return new Event() {
      @Override
      public <Result> Result visit(final EventHandler<Result> visitor) {
        return visitor.run(activityName);
      }
    };
  }

  @Override
  public final String toString() {
    return this.visit(new EventHandler<>() {
      @Override
      public String dataAdded(final String binName, final double amount) {
        return String.format("dataAdded(\"%s\", %s)",
            binName.replace("\\", "\\\\").replace("\"", "\\\""),
            amount);
      }

      @Override
      public String binCleared(final String binName) {
        return String.format("binCleared(\"%s\")",
            binName.replace("\\", "\\\\").replace("\"", "\\\""));
      }

      @Override
      public String log(final String message) {
        return String.format("log(\"%s\")",
            message.replace("\\", "\\\\").replace("\"", "\\\""));
      }

      @Override
      public String run(String activityType) {
        return String.format("run(\"%s\")",
            activityType.replace("\\", "\\\\").replace("\"", "\\\""));
      }
    });
  }
}
