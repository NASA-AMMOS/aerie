package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.driver.engine.TaskInfo;
import gov.nasa.jpl.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.ValueSchema;
import gov.nasa.jpl.aerie.time.Duration;
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
  public final Map<String, SerializedActivity> unfinishedActivities = new HashMap<>();

  public SimulationResults(
      final Map<String, List<Pair<Duration, RealDynamics>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles,
      final Map<String, String> taskIdToActivityId,
      final Map<String, TaskInfo<?>> activityRecords,
      final Instant startTime)
  {
    this.startTime = startTime;
    this.realProfiles = realProfiles;
    this.discreteProfiles = discreteProfiles;
    this.resourceSamples = takeSamples(realProfiles, discreteProfiles);
    this.simulatedActivities = buildSimulatedActivities(startTime, taskIdToActivityId, activityRecords);
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

  private Map<String, SimulatedActivity> buildSimulatedActivities(
      final Instant startTime,
      final Map<String, String> taskIdToActivityId,
      final Map<String, TaskInfo<?>> activityRecords)
  {
    final var simulatedActivities = new HashMap<String, SimulatedActivity>();
    final var activityChildren = new HashMap<String, List<String>>();

    // Create the list of children for every activity
    for (final var id : activityRecords.keySet()) {
      activityChildren.put(id, new ArrayList<>());
    }

    final var activityParents = new HashMap<String, String>();
    for (final var entry : activityRecords.entrySet()) {
      final var id = entry.getKey();
      final var activityRecord = entry.getValue();

      // An activity may have been spawned by an anonymous task.
      // In this case, we want to find the nearest ancestor that isn't anonymous,
      // and attribute this activity to it as a child.
      var ancestorId$ = activityRecord.parent;
      while (ancestorId$.isPresent()) {
        final var ancestor = activityRecords.get(ancestorId$.get());
        if (ancestor.specification.isPresent()) break;

        ancestorId$ = ancestor.parent;
      }

      ancestorId$
          .ifPresent(ancestorId -> {
            activityParents.put(id, taskIdToActivityId.get(ancestorId));
            activityChildren.get(ancestorId).add(id);
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
        this.unfinishedActivities.put(activityId, specification);
      } else {
        simulatedActivities.put(activityId, new SimulatedActivity(
            specification.getTypeName(),
            specification.getParameters(),
            Duration.addToInstant(startTime, activityRecord.startTime.get()),
            activityRecord.endTime.get().minus(activityRecord.startTime.get()),
            activityParents.getOrDefault(taskId, null),
            activityChildren.get(taskId)
        ));
      }
    }

    return simulatedActivities;
  }
}
