package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.pcollections.PVector;

public abstract class ScheduleItem<T, Activity, Event> {
  private ScheduleItem() {}

  public static final class Defer<T, Activity, Event> extends ScheduleItem<T, Activity, Event> {
    public final Duration duration;
    public final Activity activityType;
    public final PVector<ActivityBreadcrumb<T, Event>> milestones;

    public Defer(final Duration duration, final Activity activityType, final PVector<ActivityBreadcrumb<T, Event>> milestones) {
      this.duration = duration;
      this.activityType = activityType;
      this.milestones = milestones;
    }

    @Override
    public String toString() {
      return String.format("Defer(for: \"%s\")", this.duration);
    }
  }

  public static final class OnCompletion<T, Activity, Event> extends ScheduleItem<T, Activity, Event> {
    public final String waitOn;
    public final Activity activityType;
    public final PVector<ActivityBreadcrumb<T, Event>> milestones;

    public OnCompletion(final String waitOn, final Activity activityType, final PVector<ActivityBreadcrumb<T, Event>> milestones) {
      this.waitOn = waitOn;
      this.activityType = activityType;
      this.milestones = milestones;
    }

    @Override
    public String toString() {
      return String.format("OnCompletion(of: \"%s\")", this.waitOn.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }

  public static final class Complete<T, Activity, Event> extends ScheduleItem<T, Activity, Event> {
    @Override
    public String toString() {
      return "Complete";
    }
  }
}
