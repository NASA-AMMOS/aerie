package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.TaskInfo;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SimulationResults {
  public final Instant startTime;
  public final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles;
  public final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles;
  public final Map<String, List<Pair<Duration, SerializedValue>>> resourceSamples;
  public final Map<String, SimulatedActivity> simulatedActivities;
  public final Map<String, SerializedActivity> unfinishedActivities;

  public SimulationResults(
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
      final Map<String, SimulatedActivity> simulatedActivities,
      final Map<String, SerializedActivity> unfinishedActivities,
      final Instant startTime)
  {
    this.startTime = startTime;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
    this.resourceSamples = takeSamples(realProfiles, discreteProfiles);
    this.simulatedActivities = simulatedActivities;
    this.unfinishedActivities = unfinishedActivities;
  }

  public static SimulationResults create(
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
      final Map<String, String> taskIdToActivityId,
      final Map<String, TaskInfo<?>> activityRecords,
      final Instant startTime)
  {
    final var partition = buildSimulatedActivities(startTime, taskIdToActivityId, activityRecords);

    return new SimulationResults(
        realProfiles,
        discreteProfiles,
        partition.finished,
        partition.unfinished,
        startTime);
  }

  private static Map<String, List<Pair<Duration, SerializedValue>>>
  takeSamples(
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles)
  {
    final var samples = new HashMap<String, List<Pair<Duration, SerializedValue>>>();

    realProfiles.forEach((name, profile) -> {
      var elapsed = Duration.ZERO;

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

  private static PartitionedActivities buildSimulatedActivities(
      final Instant startTime,
      final Map<String, String> taskIdToActivityId,
      final Map<String, TaskInfo<?>> activityRecords)
  {
    final var partition = new PartitionedActivities();

    // Create the list of children for every activity
    final var activityChildren = new HashMap<String, List<String>>();
    for (final var taskId : activityRecords.keySet()) {
      activityChildren.put(taskId, new ArrayList<>());
    }

    final var activityParents = new HashMap<String, String>();
    for (final var entry : activityRecords.entrySet()) {
      final var taskId = entry.getKey();
      final var activityRecord = entry.getValue();

      // An activity may have been spawned by an anonymous task.
      // In this case, we want to find the nearest ancestor that isn't anonymous,
      // and attribute this activity to it as a child.
      var ancestorTaskId$ = activityRecord.parent;
      while (ancestorTaskId$.isPresent()) {
        final var ancestor = activityRecords.get(ancestorTaskId$.get());
        if (ancestor.specification.isPresent()) break;

        ancestorTaskId$ = ancestor.parent;
      }

      ancestorTaskId$
          .ifPresent(ancestorTaskId -> {
            activityParents.put(taskId, taskIdToActivityId.get(ancestorTaskId));
            activityChildren.get(ancestorTaskId).add(taskIdToActivityId.get(taskId));
          });
    }

    for (final var entry : activityRecords.entrySet()) {
      final var taskId = entry.getKey();
      final var activityRecord = entry.getValue();
      final var activityId = taskIdToActivityId.get(taskId);

      // Only report on activities, not anonymous tasks.
      // TODO: Actually do report on everything, but distinguish the two kinds of task in some way.
      //   It could be really valuable to be able to show the anonymous threads of behavior performed by an activity.
      if (activityRecord.specification.isEmpty()) continue;

      final var specification = activityRecord.specification.get();

      if (activityRecord.endTime.isEmpty()) {
        partition.unfinished.put(activityId, specification);
      } else {
        partition.finished.put(activityId, new SimulatedActivity(
            specification.getTypeName(),
            specification.getParameters(),
            Duration.addToInstant(startTime, activityRecord.startTime.get()),
            activityRecord.endTime.get().minus(activityRecord.startTime.get()),
            activityParents.getOrDefault(taskId, null),
            activityChildren.get(taskId)
        ));
      }
    }

    return partition;
  }

  private static final class PartitionedActivities {
    public final Map<String, SimulatedActivity> finished = new HashMap<>();
    public final Map<String, SerializedActivity> unfinished = new HashMap<>();
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
