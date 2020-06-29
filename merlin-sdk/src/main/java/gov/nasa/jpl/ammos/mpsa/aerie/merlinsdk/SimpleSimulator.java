package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.activities.ReplayingSimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class SimpleSimulator {
  private SimpleSimulator() {}

  public static <Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final Collection<Pair<Duration, SerializedActivity>> schedule,
      final Duration simulationDuration,
      final Duration samplingPeriod
  ) {
    return simulate(adaptation, SimulationTimeline.create(), schedule, simulationDuration, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final Collection<Pair<Duration, SerializedActivity>> schedule,
      final Duration simulationDuration,
      final Duration samplingPeriod
  ) {
    final var querier = adaptation.makeQuerier(database);
    final var simulator = new ReplayingSimulationEngine<>(database.origin(), querier::runActivity);

    // Enqueue all scheduled activities
    final var mapper = adaptation.getActivityMapper();
    for (final var entry : schedule) {
      final var startDelta = entry.getLeft();
      final var serializedInstance = entry.getRight();

      simulator.enqueue(startDelta, mapper.deserializeActivity(serializedInstance).get());
    }

    final var timestamps = new ArrayList<Duration>();
    final var timelines = new HashMap<String, List<SerializedParameter>>();
    for (final var stateName : querier.states()) {
      timelines.put(stateName, new ArrayList<>());
    }

    // Run simulation to completion, sampling states at periodic intervals.
    {
      {
        timestamps.add(simulator.getElapsedTime());
        for (final var stateName : timelines.keySet()) {
          timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
        }
      }

      final var remainder = simulationDuration.remainderOf(samplingPeriod);
      for (long i = 0; i < simulationDuration.dividedBy(samplingPeriod); ++i) {
        simulator.runFor(samplingPeriod);

        timestamps.add(simulator.getElapsedTime());
        for (final var stateName : timelines.keySet()) {
          timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
        }
      }

      if (!remainder.isZero()) {
        simulator.runFor(simulationDuration.remainderOf(samplingPeriod));

        timestamps.add(simulator.getElapsedTime());
        for (final var stateName : timelines.keySet()) {
          timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
        }
      }
    }

    final var endTime = simulator.getCurrentHistory();
    // TODO: check constraints against `endTime`

    return new SimulationResults(timestamps, timelines);
  }

  public static <Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final Collection<Pair<Duration, SerializedActivity>> schedule,
      final Duration samplingPeriod
  ) {
    return simulateToCompletion(adaptation, SimulationTimeline.create(), schedule, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final Collection<Pair<Duration, SerializedActivity>> schedule,
      final Duration samplingPeriod
  ) {
    final var querier = adaptation.makeQuerier(database);
    final var simulator = new ReplayingSimulationEngine<>(database.origin(), querier::runActivity);

    // Enqueue all scheduled activities
    final var mapper = adaptation.getActivityMapper();
    for (final var entry : schedule) {
      final var startDelta = entry.getLeft();
      final var serializedInstance = entry.getRight();

      simulator.enqueue(startDelta, mapper.deserializeActivity(serializedInstance).get());
    }

    final var timestamps = new ArrayList<Duration>();
    final var timelines = new HashMap<String, List<SerializedParameter>>();
    for (final var stateName : querier.states()) {
      timelines.put(stateName, new ArrayList<>());
    }

    // Run simulation to completion, sampling states at periodic intervals.
    {
      timestamps.add(simulator.getElapsedTime());
      for (final var stateName : timelines.keySet()) {
        timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
      }
    }
    while (simulator.hasMoreJobs()) {
      simulator.runFor(samplingPeriod);

      timestamps.add(simulator.getElapsedTime());
      for (final var stateName : timelines.keySet()) {
        timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
      }
    }

    final var endTime = simulator.getCurrentHistory();
    // TODO: check constraints against `endTime`

    return new SimulationResults(timestamps, timelines);
  }
}
