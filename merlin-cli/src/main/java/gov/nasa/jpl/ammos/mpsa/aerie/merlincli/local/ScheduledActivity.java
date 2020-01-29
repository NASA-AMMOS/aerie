package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.local;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.Objects;

public final class ScheduledActivity {
  public final Instant startTime;
  public final SerializedActivity activity;

  public ScheduledActivity(final Instant startTime, final SerializedActivity activity) {
    this.startTime = startTime;
    this.activity = activity;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof ScheduledActivity)) return false;
    final var other = (ScheduledActivity)o;

    return ( Objects.equals(this.startTime, other.startTime)
        &&   Objects.equals(this.activity, other.activity)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.startTime, this.activity);
  }
}
