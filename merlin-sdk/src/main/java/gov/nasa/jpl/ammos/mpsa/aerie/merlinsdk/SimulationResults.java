package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.activities.SimulatedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintViolation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SimulationResults {
  public final List<Duration> timestamps;
  public final Map<String, List<SerializedValue>> timelines;
  public final List<ConstraintViolation> constraintViolations;
  public final Map<String, SimulatedActivity> simulatedActivities;

  public SimulationResults(
      final List<Duration> timestamps,
      final Map<String, List<SerializedValue>> timelines,
      final List<ConstraintViolation> constraintViolations,
      final Map<String, SerializedActivity> activityMap,
      final Map<String, Pair<Duration, Duration>> activityWindows,
      final Map<String, Optional<String>> activityParents
  ) {
    this.timestamps = timestamps;
    this.timelines = timelines;
    this.constraintViolations = constraintViolations;
    this.simulatedActivities = buildSimulatedActivities(activityMap, activityWindows, activityParents);
  }

  private Map<String, SimulatedActivity> buildSimulatedActivities(
      Map<String, SerializedActivity> activityMap,
      Map<String, Pair<Duration, Duration>> activityWindows,
      Map<String, Optional<String>> activityParents
  ) {
    final var simulatedActivities = new HashMap<String, SimulatedActivity>();
    final var activityChildren = new HashMap<String, List<String>>();

    // Create the list of children for every activity
    for (final var id : activityParents.keySet()) activityChildren.put(id, new ArrayList<>());
    for (final var entry : activityParents.entrySet()) {
      if (entry.getValue().isPresent()) {
        activityChildren.get(entry.getValue().get()).add(entry.getKey());
      }
    }

    for (final var id : activityMap.keySet()) {
      final var activity = activityMap.get(id);
      final var window = activityWindows.get(id);
      final var parent = activityParents.get(id).orElse(null);
      final var children = activityChildren.get(id);
      simulatedActivities.put(id, new SimulatedActivity(
          activity.getTypeName(),
          activity.getParameters(),
          window.getLeft(),
          window.getRight().minus(window.getLeft()),
          parent,
          children
      ));
    }

    return simulatedActivities;
  }
}
