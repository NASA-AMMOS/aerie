package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;

public abstract class ActivityBreadcrumb<T, Event> {
  private ActivityBreadcrumb() {}

  public static final class Advance<T, Event> extends ActivityBreadcrumb<T, Event> {
    public final History<T, Event> next;
    public Advance(final History<T, Event> next) {
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
