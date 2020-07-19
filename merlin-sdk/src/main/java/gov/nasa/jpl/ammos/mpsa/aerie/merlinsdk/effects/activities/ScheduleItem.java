package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.PVector;

import java.util.Objects;

public abstract class ScheduleItem<Activity> {
  private ScheduleItem() {}

  public static final class Defer<Activity> extends ScheduleItem<Activity> {
    public final Duration duration;
    public final Activity activity;

    public Defer(final Duration duration, final Activity activity) {
      this.duration = Objects.requireNonNull(duration);
      this.activity = Objects.requireNonNull(activity);
    }

    @Override
    public String toString() {
      return String.format("Defer(for: \"%s\")", this.duration);
    }
  }

  public static final class OnCompletion<Activity> extends ScheduleItem<Activity> {
    public final String waitOn;
    public final Activity activity;

    public OnCompletion(final String waitOn, final Activity activity) {
      this.waitOn = waitOn;
      this.activity = Objects.requireNonNull(activity);
    }

    @Override
    public String toString() {
      return String.format("OnCompletion(of: \"%s\")", this.waitOn.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }

  public static final class Complete<Activity> extends ScheduleItem<Activity> {
    @Override
    public String toString() {
      return "Complete";
    }
  }
}
