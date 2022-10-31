package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.InstantiationException;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class SimulationDriver {

  public static <Model>
  void simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration,
      final Consumer<SimulationResults> writer
  ) {
    writer.accept(simulate(missionModel, schedule, startTime, simulationDuration));
  }

  record SimulationMetadata(
      Instant startTime,
      List<Triple<Integer, String, ValueSchema>> topics,
      Map<String, ValueSchema> realProfiles,
      Map<String, ValueSchema> discreteProfiles
  ) {}

  record SimulationSegment(
      Duration elapsedTime,
      Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      Map<String, List<Pair<Duration, SerializedValue>>> discreteProfiles,
      SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events
      // TODO activity info
  ) {}

  interface SimulationResultsWriter {
    void writeMetadata(SimulationMetadata metadata);
    void writeSegment(SimulationSegment segment);
    void writeActivityInfo(
        Map<ActivityInstanceId, SimulatedActivity> simulatedActivities,
        Map<ActivityInstanceId, UnfinishedActivity> unfinishedActivities);
  }

  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration
  ) {
    final List<SimulationMetadata> metadataList = new ArrayList<>();
    final List<SimulationSegment> segments = new ArrayList<>();
    final List<Pair<Map<ActivityInstanceId, SimulatedActivity>, Map<ActivityInstanceId, UnfinishedActivity>>> activities = new ArrayList<>();
    simulate(missionModel, schedule, startTime, simulationDuration, new SimulationResultsWriter() {
      @Override
      public void writeMetadata(final SimulationMetadata metadata$) {
        metadataList.add(metadata$);
      }

      @Override
      public void writeSegment(final SimulationSegment segment) {
        segments.add(segment);
      }

      @Override
      public void writeActivityInfo(
          final Map<ActivityInstanceId, SimulatedActivity> simulatedActivities,
          final Map<ActivityInstanceId, UnfinishedActivity> unfinishedActivities)
      {
        activities.add(Pair.of(simulatedActivities, unfinishedActivities));
      }
    });
    if (metadataList.size() != 1) {
      throw new Error("Should call writeMetadata exactly once, called it " + metadataList.size() + " times.");
    }
    if (activities.size() != 1) {
      throw new Error("Should call writeActivityInfo exactly once, called it " + activities.size() + " times.");
    }

    final var metadata = metadataList.get(0);

    final Map<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>> realProfiles = new HashMap<>();
    final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles = new HashMap<>();
    final SortedMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>> events = new TreeMap<>();

    for (final var profile : metadata.realProfiles().entrySet()) {
      realProfiles.put(profile.getKey(), Pair.of(profile.getValue(), new ArrayList<>()));
    }

    for (final var profile : metadata.discreteProfiles().entrySet()) {
      discreteProfiles.put(profile.getKey(), Pair.of(profile.getValue(), new ArrayList<>()));
    }

    for (final SimulationSegment segment : segments) {
      for (final var profile : segment.realProfiles.entrySet()) {
        realProfiles
            .get(profile.getKey())
            .getRight()
            .addAll(profile.getValue());
      }
      for (final var profile : segment.discreteProfiles.entrySet()) {
        discreteProfiles
            .get(profile.getKey())
            .getRight()
            .addAll(profile.getValue());
      }
      events.putAll(segment.events);
    }


    return new SimulationResults(
        realProfiles,
        discreteProfiles,
        activities.get(0).getLeft(),
        activities.get(0).getRight(),
        startTime,
        metadata.topics(),
        events
    );
  }

  public static <Model>
  void simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration,
      final SimulationResultsWriter writer
  ) {
    try (final var engine = new SimulationEngine()) {
      /* The top-level simulation timeline. */
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = Duration.ZERO;

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
        timeline.add(commit);
      }

      // Specify a topic on which tasks can log the activity they're associated with.
      final var activityTopic = new Topic<ActivityInstanceId>();

      // Schedule all activities.
      for (final var entry : schedule.entrySet()) {
        final var directiveId = entry.getKey();
        final var startOffset = entry.getValue().getLeft();
        final var serializedDirective = entry.getValue().getRight();

        final Task<?> task;
        try {
          task = missionModel.createTask(serializedDirective);
        } catch (final InstantiationException ex) {
          // All activity instantiations are assumed to be validated by this point
          throw new Error("Unexpected state: activity instantiation %s failed with: %s"
              .formatted(serializedDirective.getTypeName(), ex.toString()));
        }

        final var taskId = engine.scheduleTask(startOffset, emitAndThen(directiveId, activityTopic, task));
      }

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (true) {
        final var batch = engine.extractNextJobs(simulationDuration);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        if (batch.jobs().isEmpty() && batch.offsetFromStart().isEqualTo(simulationDuration)) {
          break;
        }

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration);
        timeline.add(commit);
      }

      final var topics = missionModel.getTopics();
      final var results = SimulationEngine.computeResults(
          engine,
          startTime,
          elapsedTime,
          activityTopic,
          timeline,
          topics);
      writer.writeMetadata(new SimulationMetadata(
          startTime,
          results.topics,
          results
              .realProfiles
              .entrySet()
              .stream()
              .map(entry -> Pair.of(entry.getKey(), entry.getValue().getLeft()))
              .collect(Collectors.toMap(Pair::getKey, Pair::getValue)),
          results
              .discreteProfiles
              .entrySet()
              .stream()
              .map(entry -> Pair.of(entry.getKey(), entry.getValue().getLeft()))
              .collect(Collectors.toMap(Pair::getKey, Pair::getValue))
      ));
      writer.writeSegment(new SimulationSegment(
          elapsedTime,
          results
              .realProfiles
              .entrySet()
              .stream()
              .map($ -> Pair.of($.getKey(), $.getValue().getRight()))
              .collect(Collectors.toMap(Pair::getKey, Pair::getValue)),
          results
              .discreteProfiles
              .entrySet()
              .stream()
              .map($ -> Pair.of($.getKey(), $.getValue().getRight()))
              .collect(Collectors.toMap(Pair::getKey, Pair::getValue)),
          results.events
      ));
      writer.writeActivityInfo(results.simulatedActivities, results.unfinishedActivities);
    }
  }

  public static <Model, Return>
  void simulateTask(final MissionModel<Model> missionModel, final Task<Return> task) {
    try (final var engine = new SimulationEngine()) {
      /* The top-level simulation timeline. */
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, missionModel.getInitialCells());
      /* The current real time. */
      var elapsedTime = Duration.ZERO;

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, elapsedTime);
      }

      // Start daemon task(s) immediately, before anything else happens.
      engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
      {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
        timeline.add(commit);
      }

      // Schedule all activities.
      final var taskId = engine.scheduleTask(elapsedTime, task);

      // Drive the engine until we're out of time.
      // TERMINATION: Actually, we might never break if real time never progresses forward.
      while (!engine.isTaskComplete(taskId)) {
        final var batch = engine.extractNextJobs(Duration.MAX_VALUE);

        // Increment real time, if necessary.
        final var delta = batch.offsetFromStart().minus(elapsedTime);
        elapsedTime = batch.offsetFromStart();
        timeline.add(delta);
        // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
        //   even if they occur at the same real time.

        // Run the jobs in this batch.
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE);
        timeline.add(commit);
      }
    }
  }

  private static <E, T>
  Task<T> emitAndThen(final E event, final Topic<E> topic, final Task<T> continuation) {
    return new Task<>() {
      @Override
      public TaskStatus<T> step(final Scheduler scheduler) {
        scheduler.emit(event, topic);
        return continuation.step(scheduler);
      }

      @Override
      public void release() {
        continuation.release();
      }
    };
  }
}
