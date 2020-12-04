package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.engine.TaskRecord;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.SimulatedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SimulationResults {
  public final List<Duration> timestamps;
  public final Map<String, List<SerializedValue>> timelines;
  public final List<ConstraintViolation> constraintViolations;
  public final Map<String, SimulatedActivity> simulatedActivities;
  public final Map<String, SerializedActivity> unfinishedActivities = new HashMap<>();

  public SimulationResults(
      final List<Duration> timestamps,
      final Map<String, List<SerializedValue>> timelines,
      final List<ConstraintViolation> constraintViolations,
      final Map<String, TaskRecord> activityRecords,
      final Map<String, Window> activityWindows,
      final Instant startTime)
  {
    this.timestamps = timestamps;
    this.timelines = timelines;
    this.constraintViolations = constraintViolations;
    this.simulatedActivities = buildSimulatedActivities(startTime, activityRecords, activityWindows);
  }

  private Map<String, SimulatedActivity> buildSimulatedActivities(
      final Instant startTime,
      final Map<String, TaskRecord> activityRecords,
      final Map<String, Window> activityWindows)
  {
    final var simulatedActivities = new HashMap<String, SimulatedActivity>();
    final var activityChildren = new HashMap<String, List<String>>();

    // Create the list of children for every activity
    for (final var id : activityRecords.keySet()) activityChildren.put(id, new ArrayList<>());
    for (final var entry : activityRecords.entrySet()) {
      final var parentId = entry.getValue().parentId;

      if (parentId.isPresent()) {
        activityChildren.get(parentId.get()).add(entry.getKey());
      }
    }

    for (final var id : activityRecords.keySet()) {
      final var activityRecord = activityRecords.get(id);

      if (!activityWindows.containsKey(id)) {
        this.unfinishedActivities.put(id, new SerializedActivity(activityRecord.type, activityRecord.arguments));
      } else {
        final var window = activityWindows.get(id);

        simulatedActivities.put(id, new SimulatedActivity(
            activityRecord.type,
            activityRecord.arguments,
            Duration.addToInstant(startTime, window.start),
            window.duration(),
            activityRecord.parentId.orElse(null),
            activityChildren.get(id)
        ));
      }
    }

    return simulatedActivities;
  }
}
