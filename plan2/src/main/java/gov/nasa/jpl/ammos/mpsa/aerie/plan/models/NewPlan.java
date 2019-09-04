package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NewPlan {
  public String name;
  public String adaptationId;
  public String startTimestamp;
  public String endTimestamp;
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

  @Override
  public boolean equals(final Object object) {
    if (object.getClass() != NewPlan.class) {
      return false;
    }

    final NewPlan other = (NewPlan)object;
    return
        (  Objects.equals(this.name, other.name)
        && Objects.equals(this.adaptationId, other.adaptationId)
        && Objects.equals(this.startTimestamp, other.startTimestamp)
        && Objects.equals(this.endTimestamp, other.endTimestamp)
        && Objects.equals(this.activityInstances, other.activityInstances)
        );
  }
}
