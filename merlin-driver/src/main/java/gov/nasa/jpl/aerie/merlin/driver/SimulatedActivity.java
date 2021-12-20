package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SimulatedActivity {
  public final String type;
  public final Map<String, SerializedValue> arguments;
  public final Instant start;
  public final Duration duration;
  public final String parentId;
  public final List<String> childIds;
  public final Optional<String> directiveId;
  public final Optional<SerializedValue> returnValue;

  public SimulatedActivity(
      final String type,
      final Map<String, SerializedValue> arguments,
      final Instant start,
      final Duration duration,
      final String parentId,
      final List<String> childIds,
      final Optional<String> directiveId,
      final Optional<SerializedValue> returnValue
      ) {
    this.type = type;
    this.arguments = arguments;
    this.start = start;
    this.duration = duration;
    this.parentId = parentId;
    this.childIds = childIds;
    this.directiveId = directiveId;
    this.returnValue = returnValue;
  }

  @Override
  public String toString() {
    return
        "SimulatedActivity "
        + "{ type=" + this.type
        + ", arguments=" + this.arguments
        + ", start=" + this.start
        + ", duration=" + this.duration
        + ", parentId=" + this.parentId
        + ", directiveId=" + this.directiveId
        + ", childIds=" + this.childIds
        + " }";
  }
}
