package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.EventRecord;
import gov.nasa.jpl.aerie.merlin.driver.resources.ResourceProfile;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class SimulationResults implements SimulationResultsInterface {
  public final Instant startTime;
  public final Duration duration;
  public final Map<String, ResourceProfile<RealDynamics>> realProfiles;
  public final Map<String, ResourceProfile<SerializedValue>> discreteProfiles;
  public final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities;
  public final Set<SimulatedActivityId> removedActivities;
  public final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities;
  public final List<Triple<Integer, String, ValueSchema>> topics;
  public final Map<Duration, List<EventGraph<EventRecord>>> events;

  public SimulationResults(
      final Map<String, ResourceProfile<RealDynamics>> realProfiles,
      final Map<String, ResourceProfile<SerializedValue>> discreteProfiles,
      final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities,
      final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities,
      final Instant startTime,
      final Duration duration,
      final List<Triple<Integer, String, ValueSchema>> topics,
      final SortedMap<Duration, List<EventGraph<EventRecord>>> events)
  {
    this(realProfiles, discreteProfiles, simulatedActivities, new HashSet<>(),
         unfinishedActivities, startTime, duration, topics, events);
  }

  public SimulationResults(
      final Map<String, ResourceProfile<RealDynamics>> realProfiles,
      final Map<String, ResourceProfile<SerializedValue>> discreteProfiles,
      final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities,
      final Set<SimulatedActivityId> removedActivities,
      final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities,
      final Instant startTime,
      final Duration duration,
      final List<Triple<Integer, String, ValueSchema>> topics,
      final SortedMap<Duration, List<EventGraph<EventRecord>>> events)
  {
    this.startTime = startTime;
    this.duration = duration;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
    this.topics = topics;
    this.simulatedActivities = simulatedActivities;
    this.removedActivities = removedActivities;
    this.unfinishedActivities = unfinishedActivities;
    this.events = events;
  }

  @Override
  public String toString() {
    return makeString();
  }

  @Override
  public Instant getStartTime() {
    return startTime;
  }

  @Override
  public Duration getDuration() {
    return duration;
  }

  @Override
  public Map<String, ResourceProfile<RealDynamics>> getRealProfiles() {
    return realProfiles;
  }

  @Override
  public Map<String, ResourceProfile<SerializedValue>> getDiscreteProfiles() {
    return discreteProfiles;
  }

  @Override
  public Map<SimulatedActivityId, SimulatedActivity> getSimulatedActivities() {
    return simulatedActivities;
  }

  @Override
  public Set<SimulatedActivityId> getRemovedActivities() {
    return removedActivities;
  }

  @Override
  public Map<SimulatedActivityId, UnfinishedActivity> getUnfinishedActivities() {
    return unfinishedActivities;
  }

  @Override
  public List<Triple<Integer, String, ValueSchema>> getTopics() {
    return topics;
  }

  @Override
  public Map<Duration, List<EventGraph<EventRecord>>> getEvents() {
    return events;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof SimulationResults that)) return false;

    return startTime.equals(that.startTime)
           && duration.isEqualTo(that.duration)
           && realProfiles.equals(that.realProfiles)
           && discreteProfiles.equals(that.discreteProfiles)
           && simulatedActivities.equals(that.simulatedActivities)
           && unfinishedActivities.equals(that.unfinishedActivities)
           && topics.equals(that.topics)
           && events.equals(that.events);
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
