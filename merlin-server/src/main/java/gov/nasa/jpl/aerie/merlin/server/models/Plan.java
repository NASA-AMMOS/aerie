package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Plan {
  public String name;
  public String missionModelId;
  public Timestamp startTimestamp;
  public Timestamp endTimestamp;
  public Map<ActivityDirectiveId, ActivityDirective> activityDirectives;
  public Map<String, SerializedValue> configuration = new HashMap<>();

  public Plan() {}

  public Plan(final Plan other) {
    this.name = other.name;
    this.missionModelId = other.missionModelId;
    this.startTimestamp = other.startTimestamp;
    this.endTimestamp = other.endTimestamp;

    if (other.activityDirectives != null) {
      this.activityDirectives = new HashMap<>();
      this.activityDirectives.putAll(other.activityDirectives);
    }

    if (other.configuration != null) this.configuration = new HashMap<>(other.configuration);
  }

  public Plan(
      final String name,
      final String missionModelId,
      final Timestamp startTimestamp,
      final Timestamp endTimestamp,
      final Map<ActivityDirectiveId, ActivityDirective> activityDirectives,
      final Map<String, SerializedValue> configuration
  ) {
    this.name = name;
    this.missionModelId = missionModelId;
    this.startTimestamp = startTimestamp;
    this.endTimestamp = endTimestamp;
    this.activityDirectives = (activityDirectives != null) ? Map.copyOf(activityDirectives) : null;
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
