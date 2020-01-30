package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.local;

import java.util.List;
import java.util.Objects;

public final class Schedule {
  public final String adaptationId;
  public final List<ScheduledActivity> scheduledActivities;

  public Schedule(final String adaptationId, final List<ScheduledActivity> scheduledActivities) {
    this.adaptationId = adaptationId;
    this.scheduledActivities = List.copyOf(scheduledActivities);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Schedule)) return false;
    final var other = (Schedule)o;

    return ( Objects.equals(this.adaptationId, other.adaptationId)
        &&   Objects.equals(this.scheduledActivities, other.scheduledActivities)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.adaptationId, this.scheduledActivities);
  }
}
