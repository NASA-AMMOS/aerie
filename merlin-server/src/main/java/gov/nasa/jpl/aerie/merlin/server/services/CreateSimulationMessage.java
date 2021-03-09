package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class CreateSimulationMessage {
  public final String adaptationId;
  public final Instant startTime;
  public final Duration samplingDuration;
  public final Map<String, Pair<Duration, SerializedActivity>> activityInstances;

  public CreateSimulationMessage(
      final String adaptationId,
      final Instant startTime,
      final Duration samplingDuration,
      final Map<String, Pair<Duration, SerializedActivity>> activityInstances
  ) {
    this.adaptationId = adaptationId;
    this.startTime = startTime;
    this.samplingDuration = samplingDuration;
    this.activityInstances = activityInstances;
  }

  @Override
  public String toString() {
    return "CreateSimulationMessage { " +
        "adaptationId = " + this.adaptationId + ", " +
        "startTime = " + this.startTime + ", " +
        "samplingDuration = " + this.samplingDuration + ", " +
        "activityInstances = " + this.activityInstances + " }";
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof CreateSimulationMessage)) return false;
    final var other = (CreateSimulationMessage) o;

    return
        (  Objects.equals(this.adaptationId, other.adaptationId)
        && Objects.equals(this.startTime, other.startTime)
        && Objects.equals(this.samplingDuration, other.samplingDuration)
        && Objects.equals(this.activityInstances, other.activityInstances)
        );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.adaptationId,
        this.startTime,
        this.samplingDuration,
        this.activityInstances
    );
  }
}
