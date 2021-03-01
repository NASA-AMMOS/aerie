package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;

public abstract class ActivityBreadcrumb<$Timeline> {
  private ActivityBreadcrumb() {}

  public static final class Advance<$Timeline> extends ActivityBreadcrumb<$Timeline> {
    public final Checkpoint<$Timeline> next;
    public Advance(final Checkpoint<$Timeline> next) {
      this.next = next;
    }
  }

  public static final class Spawn<T> extends ActivityBreadcrumb<T> {
    public final String activityId;
    public Spawn(final String activityId) {
      this.activityId = activityId;
    }
  }
}
