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

public final class SimulationResults {
  public final Instant startTime;
  public final Duration totalTime;
  public final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles;
  public final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles;
  public final Map<String, List<Pair<Duration, SerializedValue>>> resourceSamples;
  public final Map<String, SimulatedActivity> simulatedActivities;
  public final Map<String, SerializedActivity> unfinishedActivities;
  public final List<Pair<Duration, EventGraph<Triple<String, ValueSchema, SerializedValue>>>> events;

  public SimulationResults(
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
      final Map<String, SimulatedActivity> simulatedActivities,
      final Map<String, SerializedActivity> unfinishedActivities,
      final Instant startTime,
      final Duration totalTime,
      final List<Pair<Duration, EventGraph<Triple<String, ValueSchema, SerializedValue>>>> events)
  {
    this.startTime = startTime;
    this.totalTime = totalTime;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
    this.resourceSamples = takeSamples(realProfiles, discreteProfiles, totalTime);
    this.simulatedActivities = simulatedActivities;
    this.unfinishedActivities = unfinishedActivities;
    this.events = events;
  }

  private static Map<String, List<Pair<Duration, SerializedValue>>>
  takeSamples(
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
      final Duration totalTime)
  {
    final var samples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();

    realProfiles.forEach((name, profile) -> {
      var elapsed = Duration.ZERO;

      final var timeline = new ArrayList<Pair<Duration, SerializedValue>>();
      final var iterator = profile.iterator();
      if (iterator.hasNext()) {
        var piece = iterator.next();
        timeline.add(Pair.of(elapsed, SerializedValue.of(
            piece.getRight().initial)));

        while (iterator.hasNext()) {
          final var nextPiece = iterator.next();
          final var extent = nextPiece.getLeft();
          elapsed = elapsed.plus(extent);
          timeline.add(Pair.of(elapsed, SerializedValue.of(
              piece.getRight().initial + piece.getRight().rate * extent.ratioOver(Duration.SECONDS))));

          piece = nextPiece;
          timeline.add(Pair.of(elapsed, SerializedValue.of(
              piece.getRight().initial)));
        }

        final var extent = totalTime.minus(elapsed);
        elapsed = elapsed.plus(extent);
        timeline.add(Pair.of(elapsed, SerializedValue.of(
            piece.getRight().initial + piece.getRight().rate * extent.ratioOver(Duration.SECONDS))));
      }
      samples.put(name, timeline);
    });
    discreteProfiles.forEach((name, p) -> {
      var elapsed = Duration.ZERO;
      var profile = p.getRight();

      final var timeline = new ArrayList<Pair<Duration, SerializedValue>>();

      final var iterator = profile.iterator();
      if (iterator.hasNext()) {
        var piece = iterator.next();
        timeline.add(Pair.of(elapsed, piece.getRight()));

        while (iterator.hasNext()) {
          final var nextPiece = iterator.next();
          final var extent = nextPiece.getLeft();
          elapsed = elapsed.plus(extent);
          timeline.add(Pair.of(elapsed, piece.getRight()));

          piece = nextPiece;
          timeline.add(Pair.of(elapsed, piece.getRight()));
        }

        final var extent = totalTime.minus(elapsed);
        elapsed = elapsed.plus(extent);
        timeline.add(Pair.of(elapsed, piece.getRight()));
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
