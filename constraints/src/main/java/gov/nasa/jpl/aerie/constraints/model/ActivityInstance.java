package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import java.util.Map;
import java.util.Objects;

public final class ActivityInstance {
  public final long id;
  public final String type;
  public final Map<String, SerializedValue> parameters;
  public final Interval interval;

  public ActivityInstance(
      final long id,
      final String type,
      final Map<String, SerializedValue> parameters,
      final Interval interval) {
    this.type = type;
    this.id = id;
    this.parameters = parameters;
    this.interval = interval;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ActivityInstance)) return false;
    final var o = (ActivityInstance) obj;

    return Objects.equals(this.id, o.id)
        && Objects.equals(this.type, o.type)
        && Objects.equals(this.parameters, o.parameters)
        && Objects.equals(this.interval, o.interval);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.type, this.parameters, this.interval);
  }
}
