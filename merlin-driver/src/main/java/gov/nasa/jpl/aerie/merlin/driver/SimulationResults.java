package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public final class SimulationResults {
  public final Instant startTime;
  public final Duration duration;
  public final Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles;
  public final Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles;
  public final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities;
  public final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities;
  public final List<Triple<Integer, String, ValueSchema>> topics;
  public final Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events;

    public SimulationResults(
        final Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles,
        final Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles,
        final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities,
        final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities,
        final Instant startTime,
        final Duration duration,
        final List<Triple<Integer, String, ValueSchema>> topics,
        final SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events)
  {
    this.startTime = startTime;
    this.duration = duration;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
    this.topics = topics;
    this.simulatedActivities = simulatedActivities;
    this.unfinishedActivities = unfinishedActivities;
    this.events = events;
  }

  @Override
  public String toString() {
    return
        "SimulationResults "
        + "{ startTime=" + this.startTime
        + ", realProfiles=" + this.realProfiles
        + ", discreteProfiles=" + this.discreteProfiles
        + ", simulatedActivities=" + this.simulatedActivities
        + ", unfinishedActivities=" + this.unfinishedActivities
        + " }";
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SimulationResults that = (SimulationResults) o;

    if (!startTime.equals(that.startTime)) return false;
    if (!duration.isEqualTo(that.duration)) return false;
    if (!realProfiles.equals(that.realProfiles)) return false;
    if (!discreteProfiles.equals(that.discreteProfiles)) return false;
    if (!simulatedActivities.equals(that.simulatedActivities)) return false;
    if (!unfinishedActivities.equals(that.unfinishedActivities)) return false;
    if (!topics.equals(that.topics)) return false;
    return events.equals(that.events);
  }

  @Override
  public int hashCode() {
    int result = startTime.hashCode();
    result = 31 * result + duration.hashCode();
    result = 31 * result + realProfiles.hashCode();
    result = 31 * result + discreteProfiles.hashCode();
    result = 31 * result + simulatedActivities.hashCode();
    result = 31 * result + unfinishedActivities.hashCode();
    result = 31 * result + topics.hashCode();
    result = 31 * result + events.hashCode();
    return result;
  }
}
