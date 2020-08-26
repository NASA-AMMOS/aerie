package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ActivityInstance {
  public String type;
  public Timestamp startTimestamp;
  public Map<String, SerializedValue> parameters;

  public ActivityInstance() {}

  public ActivityInstance(final ActivityInstance other) {
    this.type = other.type;
    this.startTimestamp = other.startTimestamp;
    this.parameters = (other.parameters == null) ? null : new HashMap<>(other.parameters);
  }

  public ActivityInstance(final String type, final Timestamp startTimestamp, final Map<String, SerializedValue> parameters) {
    this.type = type;
    this.startTimestamp = startTimestamp;
    this.parameters = (parameters != null) ? Map.copyOf(parameters) : null;
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof ActivityInstance)) {
      return false;
    }

    final var other = (ActivityInstance)object;
    return
        (  Objects.equals(this.type, other.type)
        && Objects.equals(this.startTimestamp, other.startTimestamp)
        && Objects.equals(this.parameters, other.parameters)
        );
  }
}
