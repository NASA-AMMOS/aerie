package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record CreateSimulationMessage(
  String missionModelId,
  Instant startTime,
  Duration planDuration,
  Duration simulationDuration,
  Map<ActivityDirectiveId, ActivityDirective> activityDirectives,
  Map<String, SerializedValue> configuration
)
{
  public CreateSimulationMessage {
    Objects.requireNonNull(missionModelId);
    Objects.requireNonNull(startTime);
    Objects.requireNonNull(planDuration);
    Objects.requireNonNull(simulationDuration);
    Objects.requireNonNull(activityDirectives);
    Objects.requireNonNull(configuration);
  }
}
