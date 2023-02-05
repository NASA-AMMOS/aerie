package gov.nasa.jpl.aerie.scheduler.simulation;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityInstanceId;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/* package-private */ record Schedule(Map<ActivityInstanceId, Directive> activitiesById) {
  public static Schedule empty() {
    return new Schedule(Map.of());
  }

  public static Schedule of(
      final Map<SchedulingActivityInstanceId, ActivityInstanceId> planActInstanceIdToSimulationActInstanceId,
      final Plan plan) {
    return new Schedule(
        Map.copyOf(plan.getActivitiesById()
            .entrySet()
            .stream()
            .map($ -> Pair.of(
                Objects.requireNonNull(planActInstanceIdToSimulationActInstanceId.get($.getKey()), "No entry for " + $.getKey() + " in map: " + planActInstanceIdToSimulationActInstanceId),
                new Directive(new StartTime.OffsetFromPlanStart($.getValue().startTime()),
                        new SerializedActivity(
                            $.getValue().getType().getName(),
                            $.getValue().arguments()))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
  }

  public static Optional<Duration> firstDifference(final Schedule previousSchedule, final Schedule newSchedule) {
    if (previousSchedule.equals(newSchedule)) return Optional.empty();

    final HashSet<Map.Entry<ActivityInstanceId, Directive>> removed;
    final HashSet<Map.Entry<ActivityInstanceId, Directive>> added;
    {
      final var previousCopy = new HashSet<>(previousSchedule.activitiesById.entrySet());
      final var newCopy = new HashSet<>(newSchedule.activitiesById.entrySet());
      previousCopy.removeAll(newSchedule.activitiesById.entrySet());
      newCopy.removeAll(previousSchedule.activitiesById.entrySet());
      removed = previousCopy;
      added = newCopy;
    }

    if (removed.isEmpty() && added.isEmpty()) return Optional.empty();
    final var minRemoved = removed
        .stream()
        .min(Comparator.comparing(o -> ((StartTime.OffsetFromPlanStart) o.getValue().startTime).offset())).map($ -> ((StartTime.OffsetFromPlanStart) $.getValue().startTime).offset());
    final var minAdded = added
        .stream()
        .min(Comparator.comparing(o -> ((StartTime.OffsetFromPlanStart) o.getValue().startTime).offset())).map($ -> ((StartTime.OffsetFromPlanStart) $.getValue().startTime).offset());

    if (minRemoved.isEmpty()) return minAdded;
    if (minAdded.isEmpty()) return Optional.empty();

    return Optional.of(Duration.min(minRemoved.get(), minAdded.get()));
  }

  public boolean contains(final StartTime startTime, final SerializedActivity activity) {
    return this.activitiesById.containsValue(new Directive(startTime, activity));
  }

  public Schedule replace(final ActivityInstanceId activityInstanceId, final StartTime startTime, final SerializedActivity activity) {
    if (!this.activitiesById.containsKey(activityInstanceId)) {
      throw new AssertionError("Cannot replace activity instance " + activityInstanceId + " because it's not present in " + this);
    }
    if (!this.activitiesById.get(activityInstanceId).serializedActivity().getTypeName().equals(activity.getTypeName())) {
      throw new AssertionError("Cannot change the type of an activity when replacing it. Old: " + this.activitiesById.get(activityInstanceId) + " New: " + activity.getTypeName());
    }
    final var newMap = new HashMap<>(this.activitiesById);
    newMap.put(activityInstanceId, new Directive(startTime, activity));
    return new Schedule(Map.copyOf(newMap));
  }

  public record Directive(StartTime startTime, SerializedActivity serializedActivity) {}
}
