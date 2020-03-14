package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.local;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationInstant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ScheduledActivity {
  public final Instant startTime;
  public final SerializedActivity activity;

  public final static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss");

  public ScheduledActivity(final Instant startTime, final SerializedActivity activity) {
    this.startTime = startTime;
    this.activity = activity;
  }

  public ScheduledActivity(final ActivityInstance instance) throws ParseException {
    final Map<String, SerializedParameter> serializedParameters = new HashMap<>();
    final Map<String, SerializedParameter> instanceParameters = instance.getParameters();
    for (Map.Entry<String, SerializedParameter> entry : instanceParameters.entrySet()) {
      serializedParameters.put(entry.getKey(), entry.getValue());
    }

    this.startTime = SimulationInstant.ORIGIN.plus(timestampFormat.parse(instance.getStartTimestamp()).getTime(), TimeUnit.MILLISECONDS);
    this.activity = new SerializedActivity(instance.getActivityType(), serializedParameters);
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
