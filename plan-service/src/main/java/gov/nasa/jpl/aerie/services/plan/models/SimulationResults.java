package gov.nasa.jpl.aerie.services.plan.models;

import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SimulationResults {
  public final Instant startTime;
  public final Map<String, List<Pair<Duration, SerializedValue>>> resourceSamples;
  public final JsonValue constraints;
  public final JsonValue activities;

  public SimulationResults(
      final Instant startTime,
      final Map<String, List<Pair<Duration, SerializedValue>>> resourceSamples,
      final JsonValue constraints,
      final JsonValue activities)
  {
    this.startTime = startTime;
    this.resourceSamples = resourceSamples;
    this.constraints = constraints;
    this.activities = activities;
  }

  @Override
  public String toString() {
    return "SimulationResults" +
        " {" +
        " startTime=" + startTime + "," +
        " resourceSamples=" + resourceSamples + "," +
        " constraints=" + constraints + "," +
        " activities=" + activities +
        " }";
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SimulationResults)) return false;
    final var other = (SimulationResults) o;

    return
        (  Objects.equals(this.startTime, other.startTime)
        && Objects.equals(this.resourceSamples, other.resourceSamples)
        && Objects.equals(this.constraints, other.constraints)
        && Objects.equals(this.activities, other.activities)
        );
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.startTime, this.resourceSamples, this.constraints, this.activities);
  }
}
