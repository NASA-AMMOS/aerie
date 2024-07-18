package gov.nasa.jpl.aerie.constraints.model;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ActivityInstance(
    ActivityInstanceId instanceId,
    String type,
    Map<String, SerializedValue> parameters,
    Interval interval,
    Optional<ActivityDirectiveId> directiveId
) {
  public ActivityInstance(
      long id,
      String type,
      Map<String, SerializedValue> parameters,
      Interval interval
  ) {
    this(id, type, parameters, interval, Optional.empty());
  }

  public ActivityInstance(
      long id,
      String type,
      Map<String, SerializedValue> parameters,
      Interval interval,
      Optional<ActivityDirectiveId> directiveId
  ) {
    this(new ActivityInstanceId(id), type, parameters, interval, directiveId);
  }

  public long id() {
    return this.instanceId().id();
  }
}
