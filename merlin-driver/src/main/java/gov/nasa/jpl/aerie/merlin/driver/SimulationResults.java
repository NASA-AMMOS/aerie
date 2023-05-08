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

public class SimulationResults implements SimulationResultsInterface {
  protected final Instant startTime;
  protected final Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles;
  protected final Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles;
  protected final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities;
  protected final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities;
  protected final List<Triple<Integer, String, ValueSchema>> topics;
  protected final Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events;

    public SimulationResults(
        final Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfiles,
        final Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfiles,
        final Map<SimulatedActivityId, SimulatedActivity> simulatedActivities,
        final Map<SimulatedActivityId, UnfinishedActivity> unfinishedActivities,
        final Instant startTime,
        final List<Triple<Integer, String, ValueSchema>> topics,
        final SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events)
  {
    this.startTime = startTime;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
    this.topics = topics;
    this.simulatedActivities = simulatedActivities;
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
  public Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> getRealProfiles() {
    return realProfiles;
  }

  @Override
  public Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> getDiscreteProfiles() {
    return discreteProfiles;
  }

  @Override
  public Map<SimulatedActivityId, SimulatedActivity> getSimulatedActivities() {
    return simulatedActivities;
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
  public Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> getEvents() {
    return events;
  }
}
