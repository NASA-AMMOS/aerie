package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Plan {
  public String name;
  public String adaptationId;
  public String startTimestamp;
  public String endTimestamp;
  public Map<String, ActivityInstance> activityInstances;

  public Plan() {}

  public Plan(final Plan other) {
    this.name = other.name;
    this.adaptationId = other.adaptationId;
    this.startTimestamp = other.startTimestamp;
    this.endTimestamp = other.endTimestamp;

    if (other.activityInstances != null) {
      this.activityInstances = new HashMap<>();
      for (final var entry : other.activityInstances.entrySet()) {
        this.activityInstances.put(entry.getKey(), new ActivityInstance(entry.getValue()));
      }
    }
  }

  @Override
  public boolean equals(final Object object) {
    if (object.getClass() != Plan.class) {
      return false;
    }

    final Plan other = (Plan)object;
    return
        (  Objects.equals(this.name, other.name)
        && Objects.equals(this.adaptationId, other.adaptationId)
        && Objects.equals(this.startTimestamp, other.startTimestamp)
        && Objects.equals(this.endTimestamp, other.endTimestamp)
        && Objects.equals(this.activityInstances, other.activityInstances)
        );
  }
}
