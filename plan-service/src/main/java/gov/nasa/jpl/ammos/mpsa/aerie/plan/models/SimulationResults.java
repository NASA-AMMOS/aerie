package gov.nasa.jpl.ammos.mpsa.aerie.plan.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import javax.json.JsonValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SimulationResults {
  public final Instant startTime;
  public final List<Duration> timestamps;
  public final Map<String, List<SerializedValue>> timelines;
  public final JsonValue constraints;

  public SimulationResults(final Instant startTime, final List<Duration> timestamps, final Map<String, List<SerializedValue>> timelines, JsonValue constraints) {
    this.startTime = startTime;
    this.timestamps = timestamps;
    this.timelines = timelines;
    this.constraints = constraints;
  }

  @Override
  public String toString() {
    return "SimulationResults" +
        " {" +
        " startTime=" + startTime + "," +
        " timestamps=" + timestamps + "," +
        " timelines=" + timelines + "," +
        " constraints=" + constraints +
        " }";
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof SimulationResults)) return false;
    final var other = (SimulationResults) o;

    return
        (  Objects.equals(this.startTime, other.startTime)
        && Objects.equals(this.timestamps, other.timestamps)
        && Objects.equals(this.timelines, other.timelines)
        && Objects.equals(this.constraints, other.constraints)
        );
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.startTime, this.timestamps, this.timelines, this.constraints);
  }
}
