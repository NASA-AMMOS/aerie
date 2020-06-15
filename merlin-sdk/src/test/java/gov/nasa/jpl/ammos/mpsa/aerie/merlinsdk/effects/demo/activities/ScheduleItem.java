package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

public abstract class ScheduleItem {
  private ScheduleItem() {}

  public static final class Defer extends ScheduleItem {
    public final Duration duration;

    public Defer(final Duration duration) {
      this.duration = duration;
    }

    @Override
    public String toString() {
      return String.format("Defer(for: \"%s\")", this.duration);
    }
  }

  public static final class OnCompletion extends ScheduleItem {
    public final String waitOn;

    public OnCompletion(final String waitOn) {
      this.waitOn = waitOn;
    }

    @Override
    public String toString() {
      return String.format("OnCompletion(of: \"%s\")", this.waitOn.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }

  public static final class Complete extends ScheduleItem {
    @Override
    public String toString() {
      return "Complete";
    }
  }
}
