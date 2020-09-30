package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationEngine;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.TaskFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public final class SimpleSimulator {
  private SimpleSimulator() {}

  public static <Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    return simulate(adaptation, SimulationTimeline.create(), schedule, startTime, simulationDuration, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    final var mapper = adaptation.getActivityMapper();
    final var querier = adaptation.makeQuerier(database);

    final Function<Activity, SerializedActivity> serializer = (activity) ->
        mapper.serializeActivity(activity).orElseThrow();

    final var factory = new TaskFactory<>(serializer, querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    for (final var entry : schedule.entrySet()) {
      final var activityId = entry.getKey();
      final var startDelta = entry.getValue().getLeft();
      final var serializedInstance = entry.getValue().getRight();

      simulator.defer(startDelta, factory.createReplayingTask(
          activityId,
          mapper.deserializeActivity(serializedInstance).orElseThrow()));
    }

    return simulate(querier, simulator, startTime, simulationDuration, samplingPeriod, factory);
  }

  public static <Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    return simulate(
        adaptation,
        SimulationTimeline.create(),
        instanceList,
        startTime,
        simulationDuration,
        samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulate(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod
  )
  {
    final var mapper = adaptation.getActivityMapper();
    final var querier = adaptation.makeQuerier(database);

    final Function<Activity, SerializedActivity> serializer = (activity) ->
        mapper.serializeActivity(activity).orElseThrow();

    final var factory = new TaskFactory<>(serializer, querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    for (final var entry : instanceList) {
      final var startDelta = entry.getLeft();
      final var serializedInstance = entry.getRight();

      simulator.defer(
          startDelta,
          factory.createReplayingTask(mapper.deserializeActivity(serializedInstance).orElseThrow()));
    }

    return simulate(querier, simulator, startTime, simulationDuration, samplingPeriod, factory);
  }

  private static <T, Event> SimulationResults simulate(
      final MerlinAdaptation.Querier<T, Event> querier,
      final SimulationEngine<T, Event> simulator,
      final Instant startTime,
      final Duration simulationDuration,
      final Duration samplingPeriod,
      final TaskFactory<T, Event, Activity> factory
  )
  {
    final var timestamps = new ArrayList<Duration>();
    final var timelines = new HashMap<String, List<SerializedValue>>();
    for (final var stateName : querier.states()) {
      timelines.put(stateName, new ArrayList<>());
    }

    // Run simulation to completion, sampling states at periodic intervals.
    {
      // Get an initial sample.
      {
        timestamps.add(simulator.getElapsedTime());
        for (final var stateName : timelines.keySet()) {
          timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
        }
      }

      // Sample periodically until the sampling duration expires.
      final var remainder = simulationDuration.remainderOf(samplingPeriod);
      for (long i = 0; i < simulationDuration.dividedBy(samplingPeriod); ++i) {
        simulator.runFor(samplingPeriod);

        timestamps.add(simulator.getElapsedTime());
        for (final var stateName : timelines.keySet()) {
          timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
        }
      }

      // Take one last sample if the period doesn't evenly divide the duration.
      if (!remainder.isZero()) {
        simulator.runFor(simulationDuration.remainderOf(samplingPeriod));

        timestamps.add(simulator.getElapsedTime());
        for (final var stateName : timelines.keySet()) {
          timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
        }
      }

      // Simulate any remaining activities to completion; take no samples.
      while (simulator.hasMoreTasks()) simulator.step();
    }

    final var endTime = simulator.getCurrentHistory();

    return new SimulationResults(
        timestamps,
        timelines,
        querier.getConstraintViolationsAt(endTime),
        factory.getTaskRecords(),
        simulator.getTaskWindows(),
        startTime);
  }

  public static <Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration samplingPeriod
  )
  {
    return simulateToCompletion(adaptation, SimulationTimeline.create(), schedule, startTime, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final Map<String, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration samplingPeriod
  )
  {
    final var mapper = adaptation.getActivityMapper();
    final var querier = adaptation.makeQuerier(database);

    final Function<Activity, SerializedActivity> serializer = (activity) ->
        mapper.serializeActivity(activity).orElseThrow();

    final var factory = new TaskFactory<>(serializer, querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    for (final var entry : schedule.entrySet()) {
      final var activityId = entry.getKey();
      final var startDelta = entry.getValue().getLeft();
      final var serializedInstance = entry.getValue().getRight();

      simulator.defer(
          startDelta,
          factory.createReplayingTask(activityId, mapper.deserializeActivity(serializedInstance).orElseThrow()));
    }

    return simulateToCompletion(querier, simulator, startTime, samplingPeriod, factory);
  }

  public static <Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Instant startTime,
      final Duration samplingPeriod
  )
  {
    return simulateToCompletion(adaptation, SimulationTimeline.create(), instanceList, startTime, samplingPeriod);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation<Event> adaptation,
      final SimulationTimeline<T, Event> database,
      final List<Pair<Duration, SerializedActivity>> instanceList,
      final Instant startTime,
      final Duration samplingPeriod
  )
  {
    final var mapper = adaptation.getActivityMapper();
    final var querier = adaptation.makeQuerier(database);

    final Function<Activity, SerializedActivity> serializer = (activity) ->
        mapper.serializeActivity(activity).orElseThrow();

    final var factory = new TaskFactory<>(serializer, querier::runActivity);
    final var simulator = new SimulationEngine<>(database.origin());

    // Enqueue all scheduled activities
    for (final var entry : instanceList) {
      final var startDelta = entry.getLeft();
      final var serializedInstance = entry.getRight();

      simulator.defer(
          startDelta,
          factory.createReplayingTask(mapper.deserializeActivity(serializedInstance).orElseThrow()));
    }

    return simulateToCompletion(querier, simulator, startTime, samplingPeriod, factory);
  }

  // The need for this helper is documented in the standard Java tutorials.
  // https://docs.oracle.com/javase/tutorial/java/generics/capture.html
  private static <T, Event> SimulationResults simulateToCompletion(
      final MerlinAdaptation.Querier<T, Event> querier,
      final SimulationEngine<T, Event> simulator,
      final Instant startTime,
      final Duration samplingPeriod,
      final TaskFactory<T, Event, Activity> factory
  )
  {
    final var timestamps = new ArrayList<Duration>();
    final var timelines = new HashMap<String, List<SerializedValue>>();
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
    while (simulator.hasMoreTasks()) {
      simulator.runFor(samplingPeriod);

      timestamps.add(simulator.getElapsedTime());
      for (final var stateName : timelines.keySet()) {
        timelines.get(stateName).add(querier.getSerializedStateAt(stateName, simulator.getCurrentHistory()));
      }
    }

    final var endTime = simulator.getCurrentHistory();

    return new SimulationResults(
        timestamps,
        timelines,
        querier.getConstraintViolationsAt(endTime),
        factory.getTaskRecords(),
        simulator.getTaskWindows(),
        startTime);
  }
}
