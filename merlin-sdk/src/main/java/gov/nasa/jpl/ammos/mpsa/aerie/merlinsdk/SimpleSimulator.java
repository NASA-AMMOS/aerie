package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.TaskFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public final class SimpleSimulator {
  private SimpleSimulator() {}

  public static <Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    return simulate(adaptation, SimulationTimeline.create(), schedule, simulationDuration, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    final var querier = adaptation.makeQuerier(database);
    final var factory = new TaskFactory<>(querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    final var mapper = adaptation.getActivityMapper();
    for (final var entry : schedule.entrySet()) {
      final var activityId = entry.getKey();
      final var startDelta = entry.getValue().getLeft();
      final var serializedInstance = entry.getValue().getRight();

      simulator.defer(startDelta, factory.createReplayingTask(
          activityId,
          mapper.deserializeActivity(serializedInstance).get()));
    }

    return simulate(querier, simulator, simulationDuration, samplingPeriod);
  }

  public static <Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    return simulate(adaptation, SimulationTimeline.create(), instanceList, simulationDuration, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    final var querier = adaptation.makeQuerier(database);
    final var factory = new TaskFactory<>(querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    final var mapper = adaptation.getActivityMapper();
    for (final var entry : instanceList) {
      final var startDelta = entry.getLeft();
      final var serializedInstance = entry.getRight();

      simulator.defer(startDelta, factory.createReplayingTask(mapper.deserializeActivity(serializedInstance).get()));
    }

    return simulate(querier, simulator, simulationDuration, samplingPeriod);
  }

  private static <T, Event> SimulationResults simulate(
      final MerlinAdaptation.Querier<T, Event> querier,
      final SimulationEngine<T, Event> simulator,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
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
    final var constraintResults = querier.getConstraintViolationsAt(endTime);

    return new SimulationResults(timestamps, timelines, constraintResults);
  }

  public static <Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Duration samplingPeriod
  )
  {
    return simulateToCompletion(adaptation, SimulationTimeline.create(), schedule, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Duration samplingPeriod
  )
  {
    final var querier = adaptation.makeQuerier(database);
    final var factory = new TaskFactory<>(querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    final var mapper = adaptation.getActivityMapper();
    for (final var entry : schedule.entrySet()) {
      final var activityId = entry.getKey();
      final var startDelta = entry.getValue().getLeft();
      final var serializedInstance = entry.getValue().getRight();

      simulator.defer(
          startDelta,
          factory.createReplayingTask(activityId, mapper.deserializeActivity(serializedInstance).get()));
    }

    return simulateToCompletion(querier, simulator, samplingPeriod);
  }

  public static <Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Duration samplingPeriod
  )
  {
    return simulateToCompletion(adaptation, SimulationTimeline.create(), instanceList, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Duration samplingPeriod
  )
  {
    final var querier = adaptation.makeQuerier(database);
    final var factory = new TaskFactory<>(querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    final var mapper = adaptation.getActivityMapper();
    for (final var entry : instanceList) {
      final var startDelta = entry.getLeft();
      final var serializedInstance = entry.getRight();

      simulator.defer(startDelta, factory.createReplayingTask(mapper.deserializeActivity(serializedInstance).get()));
    }

    return simulateToCompletion(querier, simulator, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation.Querier<T, Event> querier,
      final SimulationEngine<T, Event> simulator,
      final Duration samplingPeriod
  )
  {
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
    final var constraintResults = querier.getConstraintViolationsAt(endTime);

    return new SimulationResults(timestamps, timelines, constraintResults);
  }
}
