package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.Objects;

public abstract class ScheduleItem<T, Event> {
  private ScheduleItem() {}

  public abstract String getTaskId();

  public static final class Defer<T, Event> extends ScheduleItem<T, Event> {
    public final Duration duration;
    public final SimulationTask<T, Event> activity;

    public Defer(final Duration duration, final SimulationTask<T, Event> activity) {
      this.duration = Objects.requireNonNull(duration);
      this.activity = Objects.requireNonNull(activity);
    }

    @Override
    public String getTaskId() {
      return this.activity.getId();
    }

    @Override
    public String toString() {
      return String.format("Defer(for: \"%s\")", this.duration);
    }
  }

  public static final class OnCompletion<T, Event> extends ScheduleItem<T, Event> {
    public final String waitOn;
    public final SimulationTask<T, Event> activity;

    public OnCompletion(final String waitOn, final SimulationTask<T, Event> activity) {
      this.waitOn = waitOn;
      this.activity = Objects.requireNonNull(activity);
    }

    @Override
    public String getTaskId() {
      return this.activity.getId();
    }

    @Override
    public String toString() {
      return String.format("OnCompletion(of: \"%s\")", this.waitOn.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }

  public static final class Complete<T, Event> extends ScheduleItem<T, Event> {
    public final String activityId;

    public Complete(final String activityId) {
      this.activityId = activityId;
    }

    @Override
    public String getTaskId() {
      return this.activityId;
    }

    @Override
    public String toString() {
      return String.format("Complete(id: \"%s\")", this.activityId.replace("\\", "\\\\").replace("\"", "\\\""));
    }
  }
}
