package gov.nasa.jpl.aerie.merlin.server.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

public final class NewPlan {
  public String name;
  public String adaptationId;
  public Timestamp startTimestamp;
  public Timestamp endTimestamp;
  public List<ActivityInstance> activityInstances;
  public Map<String, SerializedValue> configuration = new HashMap<>();

  public NewPlan() {}

  public NewPlan(final Plan template) {
    this.name = template.name;
    this.adaptationId = template.adaptationId;
    this.startTimestamp = template.startTimestamp;
    this.endTimestamp = template.endTimestamp;

    if (template.activityInstances != null) {
      this.activityInstances = new ArrayList<>();
      for (final ActivityInstance activity : template.activityInstances.values()) {
        this.activityInstances.add(new ActivityInstance(activity));
      }
    }

    if (template.configuration != null) this.configuration = new HashMap<>(template.configuration);
  }

  public NewPlan(
      final String name,
      final String adaptationId,
      final Timestamp startTimestamp,
      final Timestamp endTimestamp,
      final List<ActivityInstance> activityInstances,
      final Map<String, SerializedValue> configuration
  ) {
    this.name = name;
    this.adaptationId = adaptationId;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.activityInstances = List.copyOf(activityInstances);
    if (configuration != null) this.configuration = new HashMap<>(configuration);
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof NewPlan)) {
      return false;
    }

    final var other = (NewPlan)object;
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
