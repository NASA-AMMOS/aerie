package gov.nasa.jpl.ammos.mpsa.aerie.services.plan.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NewPlan {
  public String name;
  public String adaptationId;
  public Timestamp startTimestamp;
  public Timestamp endTimestamp;
  public List<ActivityInstance> activityInstances;

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
  }

  public NewPlan(
      final String name,
      final String adaptationId,
      final Timestamp startTimestamp,
      final Timestamp endTimestamp,
      final List<ActivityInstance> activityInstances
  ) {
    this.name = name;
    this.adaptationId = adaptationId;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.activityInstances = List.copyOf(activityInstances);
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
        );
  }
}
