package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ActivityInstance {
  public String type;
  public String startTimestamp;
  public Map<String, ActivityParameter> parameters;

  public ActivityInstance() {}

  public ActivityInstance(final ActivityInstance other) {
    this.type = other.type;
    this.startTimestamp = other.startTimestamp;

    if (other.parameters != null) {
      this.parameters = new HashMap<>();
      for (final var entry : other.parameters.entrySet()) {
        this.parameters.put(entry.getKey(), new ActivityParameter(entry.getValue()));
      }
    }
  }

  @Override
  public boolean equals(final Object object) {
    if (object.getClass() != ActivityInstance.class) {
      return false;
    }

    final ActivityInstance other = (ActivityInstance)object;
    return
        (  Objects.equals(this.type, other.type)
        && Objects.equals(this.startTimestamp, other.startTimestamp)
        && Objects.equals(this.parameters, other.parameters)
        );
  }
}
