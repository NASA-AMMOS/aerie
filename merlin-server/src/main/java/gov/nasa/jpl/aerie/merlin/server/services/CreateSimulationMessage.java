package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record CreateSimulationMessage(
  String missionModelId,
  Instant startTime,
  Duration samplingDuration,
  Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> activityInstances,
  Map<String, SerializedValue> configuration
)
{
  public CreateSimulationMessage {
    Objects.requireNonNull(missionModelId);
    Objects.requireNonNull(startTime);
    Objects.requireNonNull(samplingDuration);
    Objects.requireNonNull(activityInstances);
    Objects.requireNonNull(configuration);
  }
}
