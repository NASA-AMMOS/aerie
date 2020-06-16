package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Time;

public abstract class ActivityBreadcrumb<T, Event> {
  private ActivityBreadcrumb() {}

  public static final class Advance<T, Event> extends ActivityBreadcrumb<T, Event> {
    public final Time<T, Event> next;
    public Advance(final Time<T, Event> next) {
      this.next = next;
    }
  }

  public static final class Spawn<T, Event> extends ActivityBreadcrumb<T, Event> {
    public final String activityId;
    public Spawn(final String activityId) {
      this.activityId = activityId;
    }
  }
}
