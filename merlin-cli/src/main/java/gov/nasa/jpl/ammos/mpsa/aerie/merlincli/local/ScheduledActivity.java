package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.local;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.SimulationInstant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Objects;

public final class ScheduledActivity {
  public final SimulationInstant startTime;
  public final SerializedActivity activity;

  public final static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss");

  public ScheduledActivity(final SimulationInstant startTime, final SerializedActivity activity) {
    this.startTime = startTime;
    this.activity = activity;
  }

  public ScheduledActivity(final ActivityInstance instance) throws ParseException {
    final var millis = timestampFormat.parse(instance.getStartTimestamp()).getTime();

    this.startTime = SimulationInstant.ORIGIN.plus(Duration.of(millis, Duration.MILLISECONDS));
    this.activity = new SerializedActivity(instance.getActivityType(), Map.copyOf(instance.getParameters()));
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
