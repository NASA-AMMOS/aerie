package gov.nasa.jpl.aerie.merlin.server.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

public final class Plan {
  public String name;
  public String adaptationId;
  public Timestamp startTimestamp;
  public Timestamp endTimestamp;
  public Map<String, ActivityInstance> activityInstances;
  public Map<String, SerializedValue> configuration = new HashMap<>();

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

    if (other.configuration != null) this.configuration = new HashMap<>(other.configuration);
  }

  public Plan(
      final String name,
      final String adaptationId,
      final Timestamp startTimestamp,
      final Timestamp endTimestamp,
      final Map<String, ActivityInstance> activityInstances,
      final Map<String, SerializedValue> configuration
  ) {
    this.name = name;
    this.adaptationId = adaptationId;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.activityInstances = (activityInstances != null) ? Map.copyOf(activityInstances) : null;
    if (configuration != null) this.configuration = new HashMap<>(configuration);
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof Plan)) {
      return false;
    }

    final var other = (Plan)object;
    return
        (  Objects.equals(this.name, other.name)
        && Objects.equals(this.adaptationId, other.adaptationId)
        && Objects.equals(this.startTimestamp, other.startTimestamp)
        && Objects.equals(this.endTimestamp, other.endTimestamp)
        && Objects.equals(this.activityInstances, other.activityInstances)
        && Objects.equals(this.configuration, other.configuration)
        );
  }
}
