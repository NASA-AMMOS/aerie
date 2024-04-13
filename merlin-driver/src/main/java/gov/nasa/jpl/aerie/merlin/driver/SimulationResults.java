package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegment;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfileSegmentFromStart;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

  public static SimulationResults of(
      SimulationResultsWithoutProfiles sansProfiles,
      SimulationEngine.SerializedProfiles profiles
  )
  {
    Map<String, Pair<ValueSchema, List<ProfileSegment<RealDynamics>>>> realProfilesWithExtent = new LinkedHashMap<>();
    Map<String, Pair<ValueSchema, List<ProfileSegment<SerializedValue>>>> discreteProfilesWithExtent = new LinkedHashMap<>();

    for (final var entry : profiles.realProfiles().entrySet()) {
      final var profile = new ArrayList<ProfileSegment<RealDynamics>>();

      final var iter = entry.getValue().getRight().iterator();
      if (iter.hasNext()) {
        var segment = iter.next();
        while (iter.hasNext()) {
          final var nextSegment = iter.next();

          profile.add(new ProfileSegment<>(
              nextSegment.startOffset().minus(segment.startOffset()),
              segment.dynamics()));
          segment = nextSegment;
        }

        profile.add(new ProfileSegment<>(
            sansProfiles.duration(),
            segment.dynamics()));
      }

      realProfilesWithExtent.put(entry.getKey(), Pair.of(entry.getValue().getLeft(), profile));
    }

    for (final var entry : profiles.discreteProfiles().entrySet()) {
      final var profile = new ArrayList<ProfileSegment<SerializedValue>>();

      final var iter = entry.getValue().getRight().iterator();
      if (iter.hasNext()) {
        var segment = iter.next();
        while (iter.hasNext()) {
          final var nextSegment = iter.next();

          profile.add(new ProfileSegment<>(
              nextSegment.startOffset().minus(segment.startOffset()),
              segment.dynamics()));
          segment = nextSegment;
        }

        profile.add(new ProfileSegment<>(
            sansProfiles.duration(),
            segment.dynamics()));
      }

      discreteProfilesWithExtent.put(entry.getKey(), Pair.of(entry.getValue().getLeft(), profile));
    }

    return new SimulationResults(
        realProfilesWithExtent,
        discreteProfilesWithExtent,
        sansProfiles.simulatedActivities(),
        sansProfiles.unfinishedActivities(),
        sansProfiles.startTime(),
        sansProfiles.duration(),
        sansProfiles.topics(),
        sansProfiles.events()
    );
  }
}
