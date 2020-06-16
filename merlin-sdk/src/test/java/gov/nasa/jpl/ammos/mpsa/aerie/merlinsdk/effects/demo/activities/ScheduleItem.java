package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.pcollections.PVector;

public abstract class ScheduleItem<T, Event> {
  private ScheduleItem() {}

  public static final class Defer<T, Event> extends ScheduleItem<T, Event> {
    public final Duration duration;
    public final String activityType;
    public final PVector<Time<T, Event>> milestones;

    public Defer(final Duration duration, final String activityType, final PVector<Time<T, Event>> milestones) {
      this.duration = duration;
      this.activityType = activityType;
      this.milestones = milestones;
    }

    @Override
    public String toString() {
      return String.format("Defer(for: \"%s\")", this.duration);
    }
  }

  public static final class OnCompletion<T, Event> extends ScheduleItem<T, Event> {
    public final String waitOn;
    public final String activityType;
    public final PVector<Time<T, Event>> milestones;

    public OnCompletion(final String waitOn, final String activityType, final PVector<Time<T, Event>> milestones) {
      this.waitOn = waitOn;
      this.activityType = activityType;
      this.milestones = milestones;
    }

    @Override
    public String toString() {
      return String.format("OnCompletion(of: \"%s\")", this.waitOn.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }

  public static final class Complete<T, Event> extends ScheduleItem<T, Event> {
    @Override
    public String toString() {
      return "Complete";
    }
  }
}
