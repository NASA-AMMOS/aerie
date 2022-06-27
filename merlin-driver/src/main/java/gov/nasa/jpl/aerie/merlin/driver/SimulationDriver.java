package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.Directive;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine.ExecutionState;
import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine.TaskInfo;
import gov.nasa.jpl.aerie.merlin.driver.engine.ProfilingState;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Event;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public final class SimulationDriver {
  public static <Model>
  SimulationResults simulate(
      final MissionModel<Model> missionModel,
      final Map<ActivityInstanceId, Pair<Duration, SerializedActivity>> schedule,
      final Instant startTime,
      final Duration simulationDuration
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
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE, missionModel);
        timeline.add(commit);
      }

      // Specify a topic on which tasks can log the activity they're associated with.
      final var activityTopic = new Topic<ActivityInstanceId>();

      // Schedule all activities.
      for (final var entry : schedule.entrySet()) {
        final var directiveId = entry.getKey();
        final var startOffset = entry.getValue().getLeft();
        final var serializedDirective = entry.getValue().getRight();

        final Directive<Model, ?, ?> directive;
        try {
          directive = missionModel.instantiateDirective(serializedDirective);
        } catch (final TaskSpecType.UnconstructableTaskSpecException | MissingArgumentsException ex) {
          // All activity instantiations are assumed to be validated by this point
          throw new Error("Unexpected state: activity instantiation %s failed with: %s"
              .formatted(serializedDirective.getTypeName(), ex.toString()));
        }

        final var task = directive.createTask(missionModel.getModel());
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
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration, missionModel);
        timeline.add(commit);
      }

      return computeResults(engine, startTime, elapsedTime, activityTopic, timeline, missionModel);
    }
  }

  /** Compute a set of results from the current state of simulation. */
  // TODO: Move result extraction out of the SimulationEngine.
  //   The Engine should only need to stream events of interest to a downstream consumer.
  //   The Engine cannot be cognizant of all downstream needs.
  // TODO: Whatever mechanism replaces `computeResults` also ought to replace `isTaskComplete`.
  // TODO: Produce results for all tasks, not just those that have completed.
  //   Planners need to be aware of failed or unfinished tasks.
  public static SimulationResults computeResults(
      final SimulationEngine engine,
      final Instant startTime,
      final Duration elapsedTime,
      final Topic<ActivityInstanceId> activityTopic,
      final TemporalEventSource timeline,
      final MissionModel<?> missionModel
  ) {

    // Collect per-task information from the event graph.
    final var taskInfo = new TaskInfo();

    for (final var point : timeline) {
      if (!(point instanceof TemporalEventSource.TimePoint.Commit p)) continue;

      final var trait = new TaskInfo.Trait(missionModel, activityTopic);
      p.events().evaluate(trait, trait::atom).accept(taskInfo);
    }

    // Extract profiles for every resource.
    final var realProfiles = new HashMap<String, List<Pair<Duration, RealDynamics>>>();
    final var discreteProfiles = new HashMap<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>();

    engine.getResources().forEach((id, state) -> {
      final var name = id.id();
      final var resource = state.resource();

      switch (resource.getType()) {
        case "real" -> realProfiles.put(
            name,
            serializeProfile(elapsedTime, state, SimulationDriver::extractRealDynamics));

        case "discrete" -> discreteProfiles.put(
            name,
            Pair.of(
                state.resource().getSchema(),
                serializeProfile(elapsedTime, state, Resource::serialize)));

        default ->
            throw new IllegalArgumentException(
                "Resource `%s` has unknown type `%s`".formatted(name, resource.getType()));
      }
    });

    // Give every task corresponding to a child activity an ID that doesn't conflict with any root activity.
    final var taskToPlannedDirective = new HashMap<>(taskInfo.taskToPlannedDirective());
    final var usedActivityInstanceIds =
        taskToPlannedDirective
            .values()
            .stream()
            .map(ActivityInstanceId::id)
            .collect(Collectors.toSet());

    // Grab all tasks that are activities, so we don't have to check later
    final var activityTasks =
        engine
            .getTasks()
            .entrySet()
            .stream()
            .filter(e -> taskInfo.isActivity(e.getKey()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    var counter = 1L;
    for (final var task : activityTasks.keySet()) {
      if (taskToPlannedDirective.containsKey(task.id())) continue;

      while (usedActivityInstanceIds.contains(counter)) counter++;
      taskToPlannedDirective.put(task.id(), new ActivityInstanceId(counter++));
    }

    // Identify the nearest ancestor *activity* (excluding intermediate anonymous tasks).
    final var activityParents = new HashMap<ActivityInstanceId, ActivityInstanceId>();
    final var taskParent = engine.getTaskParent();
    activityTasks.forEach((task, state) -> {
      var parent = taskParent.get(task);
      while (parent != null && !taskInfo.isActivity(parent)) {
        parent = taskParent.get(parent);
      }

      if (parent != null) {
        activityParents.put(taskToPlannedDirective.get(task.id()), taskToPlannedDirective.get(parent.id()));
      }
    });

    final var activityChildren = new HashMap<ActivityInstanceId, List<ActivityInstanceId>>();
    activityParents.forEach((task, parent) -> {
      activityChildren.computeIfAbsent(parent, $ -> new LinkedList<>()).add(task);
    });

    final var simulatedActivities = new HashMap<ActivityInstanceId, SimulatedActivity>();
    final var unfinishedActivities = new HashMap<ActivityInstanceId, UnfinishedActivity>();
    activityTasks.forEach((task, state) -> {
      final var activityId = taskToPlannedDirective.get(task.id());

      if (state instanceof ExecutionState.Terminated<?> e) {
        final var inputAttributes = taskInfo.input().get(task.id());
        final var outputAttributes = taskInfo.output().get(task.id());

        simulatedActivities.put(activityId, new SimulatedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            e.joinOffset().minus(e.startOffset()),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(activityId),
            outputAttributes
        ));
      } else if (state instanceof ExecutionState.InProgress<?> e){
        final var inputAttributes = taskInfo.input().get(task.id());
        unfinishedActivities.put(activityId, new UnfinishedActivity(
            inputAttributes.getTypeName(),
            inputAttributes.getArguments(),
            startTime.plus(e.startOffset().in(Duration.MICROSECONDS), ChronoUnit.MICROS),
            activityParents.get(activityId),
            activityChildren.getOrDefault(activityId, Collections.emptyList()),
            (activityParents.containsKey(activityId)) ? Optional.empty() : Optional.of(activityId)
        ));
      }
    });

    final List<Triple<Integer, String, ValueSchema>> topics = new ArrayList<>();
    final var serializableTopicToId = new HashMap<MissionModel.SerializableTopic<?>, Integer>();
    for (final var serializableTopic : missionModel.getTopics()) {
      serializableTopicToId.put(serializableTopic, topics.size());
      topics.add(Triple.of(topics.size(), serializableTopic.name(), serializableTopic.valueSchema()));
    }

    final var serializedTimeline = new TreeMap<Duration, List<EventGraph<Pair<Integer, SerializedValue>>>>();
    var time = Duration.ZERO;
    for (var point : timeline.points()) {
      if (point instanceof TemporalEventSource.TimePoint.Delta delta) {
        time = time.plus(delta.delta());
      } else if (point instanceof TemporalEventSource.TimePoint.Commit commit) {
        final var serializedEventGraph = commit.events().substitute(
            event -> {
              EventGraph<Pair<Integer, SerializedValue>> output = EventGraph.empty();
              for (final var serializableTopic : missionModel.getTopics()) {
                Optional<SerializedValue> serializedEvent = trySerializeEvent(event, serializableTopic);
                if (serializedEvent.isPresent()) {
                  output = EventGraph.concurrently(output, EventGraph.atom(Pair.of(serializableTopicToId.get(serializableTopic), serializedEvent.get())));
                }
              }
              return output;
            }
        ).evaluate(new EventGraph.IdentityTrait<>(), EventGraph::atom);
        if (!(serializedEventGraph instanceof EventGraph.Empty)) {
          serializedTimeline
              .computeIfAbsent(time, x -> new ArrayList<>())
              .add(serializedEventGraph);
        }
      }
    }

    return new SimulationResults(realProfiles,
                                 discreteProfiles,
                                 simulatedActivities,
                                 unfinishedActivities,
                                 startTime,
                                 topics,
                                 serializedTimeline);
  }

  private static <EventType> Optional<SerializedValue> trySerializeEvent(Event event, MissionModel.SerializableTopic<EventType> serializableTopic) {
    return event.extract(serializableTopic.topic(), serializableTopic.serializer());
  }

  private static <Dynamics>
  RealDynamics extractRealDynamics(final Resource<Dynamics> resource, final Dynamics dynamics) {
    final var serializedSegment = resource.serialize(dynamics).asMap().orElseThrow();
    final var initial = serializedSegment.get("initial").asReal().orElseThrow();
    final var rate = serializedSegment.get("rate").asReal().orElseThrow();

    return RealDynamics.linear(initial, rate);
  }

  private static <Target, Dynamics>
  List<Pair<Duration, Target>> serializeProfile(
      final Duration elapsedTime,
      final ProfilingState<Dynamics> state,
      final Translator<Target> translator
  ) {
    final var profile = new ArrayList<Pair<Duration, Target>>(state.profile().segments().size());

    final var iter = state.profile().segments().iterator();
    if (iter.hasNext()) {
      var segment = iter.next();
      while (iter.hasNext()) {
        final var nextSegment = iter.next();

        profile.add(Pair.of(
            nextSegment.startOffset().minus(segment.startOffset()),
            translator.apply(state.resource(), segment.dynamics())));
        segment = nextSegment;
      }

      profile.add(Pair.of(
          elapsedTime.minus(segment.startOffset()),
          translator.apply(state.resource(), segment.dynamics())));
    }

    return profile;
  }

  private interface Translator<Target> {
    <Dynamics> Target apply(Resource<Dynamics> resource, Dynamics dynamics);
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
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE, missionModel);
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
        final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, Duration.MAX_VALUE, missionModel);
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
