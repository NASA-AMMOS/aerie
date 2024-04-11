package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.SimulationEngine;
import gov.nasa.jpl.aerie.merlin.driver.engine.SlabList;
import gov.nasa.jpl.aerie.merlin.driver.engine.SpanId;
import gov.nasa.jpl.aerie.merlin.driver.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.TemporalEventSource;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.nasa.jpl.aerie.merlin.driver.SimulationDriver.scheduleActivities;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.min;

public class CheckpointSimulationDriver {
  private static final Logger LOGGER = LoggerFactory.getLogger(CheckpointSimulationDriver.class);

  public record SimulationResultsComputerInputs(
      SimulationEngine engine,
      Instant simulationStartTime,
      Duration elapsedTime,
      Topic<ActivityDirectiveId> activityTopic,
      TemporalEventSource timeline,
      Iterable<MissionModel.SerializableTopic<?>> serializableTopics,
      Map<ActivityDirectiveId, SpanId> activityDirectiveIdTaskIdMap){}

  public record CachedSimulationEngine(
      Duration endsAt,
      Map<ActivityDirectiveId, ActivityDirective> activityDirectives,
      SimulationEngine simulationEngine,
      LiveCells cells,
      SlabList<TemporalEventSource.TimePoint> timePoints,
      Topic<ActivityDirectiveId> activityTopic,
      MissionModel<?> missionModel
  ) {
    public CachedSimulationEngine {
      cells.freeze();
      timePoints.freeze();
      simulationEngine.close();
    }

    public static CachedSimulationEngine empty(final MissionModel<?> missionModel) {
      final SimulationEngine engine = new SimulationEngine();
      final TemporalEventSource timeline = new TemporalEventSource();
      final LiveCells cells = new LiveCells(timeline, missionModel.getInitialCells());

      // Begin tracking all resources.
      for (final var entry : missionModel.getResources().entrySet()) {
        final var name = entry.getKey();
        final var resource = entry.getValue();

        engine.trackResource(name, resource, Duration.ZERO);
      }

      {
        // Start daemon task(s) immediately, before anything else happens.
        engine.scheduleTask(Duration.ZERO, missionModel.getDaemon());
        {
          final var batch = engine.extractNextJobs(Duration.MAX_VALUE);
          final var commit = engine.performJobs(batch.jobs(), cells, Duration.ZERO, Duration.MAX_VALUE);
          timeline.add(commit);
        }
      }

      return new CachedSimulationEngine(
          Duration.MIN_VALUE,
          Map.of(),
          engine,
          cells,
          timeline.points(),
          new Topic<>(),
          missionModel
      );
    }
  }

  /**
   * Selects the best cached engine for simulating a given plan.
   * @param schedule the schedule/plan
   * @param cachedEngines a list of cached engines
   * @return the best cached engine as well as the map of corresponding activity ids for this engine
   */
  public static Optional<Pair<CachedSimulationEngine, Map<ActivityDirectiveId, ActivityDirectiveId>>> bestCachedEngine(
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final List<CachedSimulationEngine> cachedEngines) {
    Optional<CachedSimulationEngine> bestCandidate = Optional.empty();
    final Map<ActivityDirectiveId, ActivityDirectiveId> correspondenceMap = new HashMap<>();
    for (final var cachedEngine : cachedEngines) {
      if (bestCandidate.isPresent() && cachedEngine.endsAt().noLongerThan(bestCandidate.get().endsAt()))
        continue;

      final var activityDirectivesInCache = new HashMap<>(cachedEngine.activityDirectives());
      // Find the invalidation time
      var invalidationTime = Duration.MAX_VALUE;
      final var scheduledActivities = new HashMap<>(schedule);
      for (final var activity : scheduledActivities.entrySet()) {
        if (activityDirectivesInCache.values().contains(activity.getValue())) {
          final var removedEntry =
              removeValueInMap(activityDirectivesInCache, activity.getValue());
          correspondenceMap.put(activity.getKey(), removedEntry.get().getKey());
        } else {
          invalidationTime = min(invalidationTime, activity.getValue().startOffset());
        }
      }
      for (final var activity : activityDirectivesInCache.values()) {
        invalidationTime = min(invalidationTime, activity.startOffset());
      }
      if (cachedEngine.endsAt().shorterThan(invalidationTime)) {
        bestCandidate = Optional.of(cachedEngine);
      }
    }

    bestCandidate.ifPresent(cachedSimulationEngine -> System.out.println("Re-using simulation engine at "
                                                                         + cachedSimulationEngine.endsAt()));
    return bestCandidate.map(cachedSimulationEngine -> Pair.of(cachedSimulationEngine, correspondenceMap));
  }

  private static <K,V> Optional<Map.Entry<K,V>> removeValueInMap(final Map<K,V> map, V value){
    final var it = map.entrySet().iterator();
    while(it.hasNext()){
      final var entry = it.next();
      if(entry.getValue().equals(value)){
        it.remove();
        return Optional.of(entry);
      }
    }
    return Optional.empty();
  }

  private static TemporalEventSource makeCombinedTimeline(List<TemporalEventSource> timelines, TemporalEventSource timeline) {
    final TemporalEventSource combinedTimeline = new TemporalEventSource();
    for (final var entry : timelines) {
      for (final var timePoint : entry.points()) {
        if (timePoint instanceof TemporalEventSource.TimePoint.Delta t) {
          combinedTimeline.add(t.delta());
        } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit t) {
          combinedTimeline.add(t.events());
        }
      }
    }

    for (final var timePoint : timeline) {
      if (timePoint instanceof TemporalEventSource.TimePoint.Delta t) {
        combinedTimeline.add(t.delta());
      } else if (timePoint instanceof TemporalEventSource.TimePoint.Commit t) {
        combinedTimeline.add(t.events());
      }
    }
    return combinedTimeline;
  }

  public static BiFunction<Duration, Duration, Boolean> desiredCheckpoints(final List<Duration> desiredCheckpoints) {
    return (elapsedTime, nextTime) -> {
      for (final var desiredCheckpoint : desiredCheckpoints) {
        if (elapsedTime.noLongerThan(desiredCheckpoint) && nextTime.longerThan(desiredCheckpoint)) {
          return true;
        }
      }
      return false;
    };
  }

  public static BiFunction<Duration, Duration, Boolean> wallClockCheckpoints(final long thresholdSeconds) {
    MutableLong lastCheckpointRealTime = new MutableLong(System.nanoTime());
    MutableObject<Duration> lastCheckpointSimTime = new MutableObject<>(Duration.ZERO);
    return (elapsedTime, nextTime) -> {
      if (nextTime.longerThan(elapsedTime) && System.nanoTime() - lastCheckpointRealTime.getValue() > (thresholdSeconds * 1000 * 1000 * 1000)) {
        lastCheckpointRealTime.setValue(System.nanoTime());
        lastCheckpointSimTime.setValue(elapsedTime);
        return true;
      } else {
        return false;
      }
    };
  }

  public static <Model> SimulationResultsComputerInputs simulateWithCheckpoints(
      final MissionModel<Model> missionModel,
      final Map<ActivityDirectiveId, ActivityDirective> schedule,
      final Instant simulationStartTime,
      final Duration simulationDuration,
      final Instant planStartTime,
      final Duration planDuration,
      final Consumer<Duration> simulationExtentConsumer,
      final Supplier<Boolean> simulationCanceled,
      final CachedSimulationEngine cachedEngine,
      final BiFunction<Duration, Duration, Boolean> shouldTakeCheckpoint,
      final TriFunction<SimulationEngine, Map<ActivityDirectiveId,  ActivityDirective>, Map<ActivityDirectiveId, SpanId>, Boolean> stopConditionOnPlan,
      final CachedEngineStore cachedEngineStore,
      final SimulationEngineConfiguration configuration) {
    final var activityToSpan = new HashMap<ActivityDirectiveId, SpanId>();
    final var activityTopic = cachedEngine.activityTopic();
    final var timelines = new ArrayList<TemporalEventSource>();
    timelines.add(new TemporalEventSource(cachedEngine.timePoints));
    var engine = cachedEngine.simulationEngine.duplicate();
    engine.unscheduleAfter(cachedEngine.endsAt);
    try (var ignored = cachedEngine.simulationEngine) {
      var timeline = new TemporalEventSource();
      var cells = new LiveCells(timeline, cachedEngine.cells());
      /* The current real time. */
      var elapsedTime = Duration.max(ZERO, cachedEngine.endsAt());

      simulationExtentConsumer.accept(elapsedTime);

      // Specify a topic on which tasks can log the activity they're associated with.

      try {
        final var filteredSchedule = new HashMap<ActivityDirectiveId, ActivityDirective>();
        for (final var entry : schedule.entrySet()) {
          if (entry.getValue().startOffset().longerThan(cachedEngine.endsAt())) {
            filteredSchedule.put(entry.getKey(), entry.getValue());
          }
        }

        // Get all activities as close as possible to absolute time
        // Schedule all activities.
        // Using HashMap explicitly because it allows `null` as a key.
        // `null` key means that an activity is not waiting on another activity to finish to know its start time
        HashMap<ActivityDirectiveId, List<Pair<ActivityDirectiveId, Duration>>> resolved = new StartOffsetReducer(planDuration, filteredSchedule).compute();
        if(resolved.size() != 0) {
          resolved.put(
              null,
              StartOffsetReducer.adjustStartOffset(
                  resolved.get(null),
                  Duration.of(
                      planStartTime.until(simulationStartTime, ChronoUnit.MICROS),
                      Duration.MICROSECONDS)));
        }
        // Filter out activities that are before simulationStartTime
        resolved = StartOffsetReducer.filterOutNegativeStartOffset(resolved);

        activityToSpan.putAll(scheduleActivities(
                                  filteredSchedule,
                                  resolved,
                                  missionModel,
                                  engine,
                                  activityTopic
                              )
        );

        // Drive the engine until we're out of time.
        // TERMINATION: Actually, we might never break if real time never progresses forward.
        while (elapsedTime.noLongerThan(simulationDuration) && !simulationCanceled.get()) {
          final var nextTime = engine.peekNextTime().orElse(Duration.MAX_VALUE);
          if (shouldTakeCheckpoint.apply(elapsedTime, nextTime)) {
            cells.freeze();
            LOGGER.info("Saving a simulation engine in memory");
            cachedEngineStore.save(
                new CachedSimulationEngine(
                elapsedTime,
                schedule,
                engine,
                cells,
                makeCombinedTimeline(timelines, timeline).points(),
                activityTopic,
                missionModel),
                configuration);
            timelines.add(timeline);
            engine = engine.duplicate();
            timeline = new TemporalEventSource();
            cells = new LiveCells(timeline, cells);
          }
          //break before changing the state of the engine
          if (simulationCanceled.get() || stopConditionOnPlan.apply(engine, schedule, activityToSpan)) {
            break;
          }

          final var batch = engine.extractNextJobs(simulationDuration);
          // Increment real time, if necessary.
          final var delta = batch.offsetFromStart().minus(elapsedTime);
          elapsedTime = batch.offsetFromStart();
          timeline.add(delta);
          // TODO: Advance a dense time counter so that future tasks are strictly ordered relative to these,
          //   even if they occur at the same real time.

          simulationExtentConsumer.accept(elapsedTime);

          //this break depends on the state of the batch: this is the soonest we can exist for that reason
          if (batch.jobs().isEmpty() && (batch.offsetFromStart().isEqualTo(simulationDuration))) {
            break;
          }

          // Run the jobs in this batch.
          final var commit = engine.performJobs(batch.jobs(), cells, elapsedTime, simulationDuration);
          timeline.add(commit);
        }
      } catch (Throwable ex) {
        throw new SimulationException(elapsedTime, simulationStartTime, ex);
      }

      return new SimulationResultsComputerInputs(
              engine,
              simulationStartTime,
              elapsedTime,
              activityTopic,
              makeCombinedTimeline(timelines, timeline),
              missionModel.getTopics(),
              activityToSpan);
    }
  }

  public static TriFunction<SimulationEngine, Map<ActivityDirectiveId,  ActivityDirective>,Map<ActivityDirectiveId, SpanId>, Boolean>
  stopOnceAllActivitiessAreFinished(){
    return (engine, schedule, actIdToTaskId) -> actIdToTaskId
        .values()
        .stream()
        .allMatch(engine::spanIsComplete);
  }

  public static TriFunction<SimulationEngine, Map<ActivityDirectiveId,  ActivityDirective>,Map<ActivityDirectiveId, SpanId>, Boolean>
  noCondition(){
    return (engine, schedule, actIdToSpanId) -> false;
  }

  public static TriFunction<SimulationEngine, Map<ActivityDirectiveId,  ActivityDirective>,Map<ActivityDirectiveId, SpanId>, Boolean>
  stopOnceActivityHasFinished(final ActivityDirectiveId activityDirectiveId){
    return (engine, schedule, actIdToSpanId) -> (actIdToSpanId.containsKey(activityDirectiveId)
                                                 && engine.spanIsComplete(actIdToSpanId.get(activityDirectiveId)));
  }

  public static SimulationResults computeResults(
      final SimulationResultsComputerInputs simulationResultsInputs,
      final Set<String> resourceNames){
    return SimulationEngine.computeResults(
        simulationResultsInputs.engine(),
        simulationResultsInputs.simulationStartTime(),
        simulationResultsInputs.elapsedTime(),
        simulationResultsInputs.activityTopic(),
        simulationResultsInputs.timeline(),
        simulationResultsInputs.serializableTopics(),
        resourceNames
    );
  }

  public static SimulationResults computeResults(
      final SimulationResultsComputerInputs simulationResultsInputs){
    return SimulationEngine.computeResults(
        simulationResultsInputs.engine(),
        simulationResultsInputs.simulationStartTime(),
        simulationResultsInputs.elapsedTime(),
        simulationResultsInputs.activityTopic(),
        simulationResultsInputs.timeline(),
        simulationResultsInputs.serializableTopics()
    );
  }

  public static SimulationEngine.SimulationActivityExtract computeActivitySimulationResults(
      final SimulationResultsComputerInputs simulationResultsInputs){
    return SimulationEngine.computeActivitySimulationResults(
        simulationResultsInputs.engine(),
        simulationResultsInputs.simulationStartTime(),
        simulationResultsInputs.elapsedTime(),
        simulationResultsInputs.activityTopic(),
        simulationResultsInputs.timeline(),
        simulationResultsInputs.serializableTopics());
  }
}
