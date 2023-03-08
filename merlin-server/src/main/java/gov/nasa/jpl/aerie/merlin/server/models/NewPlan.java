package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class NewPlan {
  public String name;
  public String missionModelId;
  public Timestamp startTimestamp;
  public Timestamp endTimestamp;
  public List<ActivityDirective> activityDirectives;
  public Map<String, SerializedValue> configuration = new HashMap<>();

  public NewPlan() {}

  public NewPlan(final Plan template) {
    this.name = template.name;
    this.missionModelId = template.missionModelId;
    this.startTimestamp = template.startTimestamp;
    this.endTimestamp = template.endTimestamp;

    if (template.activityDirectives != null) {
      this.activityDirectives = new ArrayList<>();
      this.activityDirectives.addAll(template.activityDirectives.values());
    }
  }

  public NewPlan(
      final String name,
      final String missionModelId,
      final Timestamp startTimestamp,
      final Timestamp endTimestamp,
      final List<ActivityDirective> activityDirectives,
      final Map<String, SerializedValue> configuration
  ) {
    this.name = name;
    this.missionModelId = missionModelId;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.activityDirectives = List.copyOf(activityDirectives);
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
        && Objects.equals(this.missionModelId, other.missionModelId)
        && Objects.equals(this.startTimestamp, other.startTimestamp)
        && Objects.equals(this.endTimestamp, other.endTimestamp)
        && Objects.equals(this.activityDirectives, other.activityDirectives)
        && Objects.equals(this.configuration, other.configuration)
        );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        name,
        missionModelId,
        startTimestamp,
        endTimestamp,
        activityDirectives,
        configuration
    );
  }
}
