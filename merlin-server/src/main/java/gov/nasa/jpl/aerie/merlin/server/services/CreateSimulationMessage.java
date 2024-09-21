package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * message requests a simulation run
 *
 * @param simReuseStrategy how to reuse prior simulations to speed up the current simulation request
 */
public record CreateSimulationMessage(
  String missionModelId,
  Instant simulationStartTime,
  Duration simulationDuration,
  Instant planStartTime,
  Duration planDuration,
  Map<ActivityDirectiveId, ActivityDirective> activityDirectives,
  Map<String, SerializedValue> configuration,
  SimulationReuseStrategy simReuseStrategy
)
{
  public CreateSimulationMessage {
    Objects.requireNonNull(missionModelId);
    Objects.requireNonNull(simulationStartTime);
    Objects.requireNonNull(simulationDuration);
    Objects.requireNonNull(planStartTime);
    Objects.requireNonNull(planDuration);
    Objects.requireNonNull(activityDirectives);
    Objects.requireNonNull(configuration);
    Objects.requireNonNull(simReuseStrategy);
  }
}
