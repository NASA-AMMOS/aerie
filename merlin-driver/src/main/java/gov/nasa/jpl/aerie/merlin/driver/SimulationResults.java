package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public final class SimulationResults {
  public final Instant startTime;
  public final Map<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>> realProfiles;
  public final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles;
  public final Map<String, List<Pair<Duration, SerializedValue>>> resourceSamples;
  public final Map<ActivityInstanceId, SimulatedActivity> simulatedActivities;
  public final Map<ActivityInstanceId, UnfinishedActivity> unfinishedActivities;
  public final List<Triple<Integer, String, ValueSchema>> topics;
  public final Map<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events;

    public SimulationResults(
        final Map<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>> realProfiles,
        final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
        final Map<ActivityInstanceId, SimulatedActivity> simulatedActivities,
        final Map<ActivityInstanceId, UnfinishedActivity> unfinishedActivities,
        final Instant startTime,
        final List<Triple<Integer, String, ValueSchema>> topics,
        final SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events)
  {
    this.startTime = startTime;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
    this.topics = topics;
    this.resourceSamples = takeSamples(realProfiles, discreteProfiles);
    this.simulatedActivities = simulatedActivities;
    this.unfinishedActivities = unfinishedActivities;
    this.events = events;
  }

  private static Map<String, List<Pair<Duration, SerializedValue>>>
  takeSamples(
      final Map<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles)
  {
    final var samples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();

    realProfiles.forEach((name, p) -> {
      var elapsed = Duration.ZERO;
      var profile = p.getRight();

      final var timeline = new ArrayList<Pair<Duration, SerializedValue>>();
      for (final var piece : profile) {
        final var extent = piece.getLeft();
        final var dynamics = piece.getRight();

        timeline.add(Pair.of(elapsed, SerializedValue.of(
            dynamics.initial)));
        elapsed = elapsed.plus(extent);
        timeline.add(Pair.of(elapsed, SerializedValue.of(
            dynamics.initial + dynamics.rate * extent.ratioOver(Duration.SECONDS))));
      }

      samples.put(name, timeline);
    });
    discreteProfiles.forEach((name, p) -> {
      var elapsed = Duration.ZERO;
      var profile = p.getRight();

      final var timeline = new ArrayList<Pair<Duration, SerializedValue>>();
      for (final var piece : profile) {
        final var extent = piece.getLeft();
        final var value = piece.getRight();

        timeline.add(Pair.of(elapsed, value));
        elapsed = elapsed.plus(extent);
        timeline.add(Pair.of(elapsed, value));
      }

      samples.put(name, timeline);
    });

    return samples;
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
}
