package gov.nasa.jpl.aerie.scheduler.model;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.tree.ProfileExpression;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirective;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @param id unique id
 * @param type the descriptor for the behavior invoked by this activity instance
 * @param startOffset the time at which this activity instance is scheduled to start
 * @param duration the length of time this activity instances lasts for after its start
 * @param arguments arguments are stored in a String/SerializedValue hashmap.
 * @param topParent the parent activity if any
 */
public record SchedulingActivityDirective(
    SchedulingActivityDirectiveId id,
    ActivityType type,
    Duration startOffset,
    Duration duration,
    Map<String, SerializedValue> arguments,
    SchedulingActivityDirectiveId topParent,
    SchedulingActivityDirectiveId anchorId,
    boolean anchoredToStart) {

  private static final AtomicLong uniqueId = new AtomicLong();

  /**
   * creates a new unscheduled activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   */
  // TODO: reconsider unscheduled activity instances
  public static SchedulingActivityDirective of(
      ActivityType type, SchedulingActivityDirectiveId anchorId, boolean anchoredToStart) {
    return new SchedulingActivityDirective(
        new SchedulingActivityDirectiveId(uniqueId.getAndIncrement()),
        type,
        Duration.ZERO,
        Duration.ZERO,
        Map.of(),
        null,
        anchorId,
        anchoredToStart);
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   * @param startOffset IN the time at which the activity is scheduled
   */
  public static SchedulingActivityDirective of(
      ActivityType type,
      Duration startOffset,
      SchedulingActivityDirectiveId anchorId,
      boolean anchoredToStart) {
    return new SchedulingActivityDirective(
        new SchedulingActivityDirectiveId(uniqueId.getAndIncrement()),
        type,
        startOffset,
        Duration.ZERO,
        Map.of(),
        null,
        anchorId,
        anchoredToStart);
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   * @param startOffset IN the time at which the activity is scheduled
   * @param duration IN the duration that the activity lasts for
   */
  public static SchedulingActivityDirective of(
      ActivityType type,
      Duration startOffset,
      Duration duration,
      SchedulingActivityDirectiveId anchorId,
      boolean anchoredToStart) {
    return new SchedulingActivityDirective(
        new SchedulingActivityDirectiveId(uniqueId.getAndIncrement()),
        type,
        startOffset,
        duration,
        Map.of(),
        null,
        anchorId,
        anchoredToStart);
  }

  public static SchedulingActivityDirective of(
      ActivityType type,
      Duration startOffset,
      Duration duration,
      Map<String, SerializedValue> parameters,
      SchedulingActivityDirectiveId anchorId,
      boolean anchoredToStart) {
    return new SchedulingActivityDirective(
        new SchedulingActivityDirectiveId(uniqueId.getAndIncrement()),
        type,
        startOffset,
        duration,
        parameters,
        null,
        anchorId,
        anchoredToStart);
  }

  public static SchedulingActivityDirective of(
      ActivityType type,
      Duration startOffset,
      Duration duration,
      Map<String, SerializedValue> parameters,
      SchedulingActivityDirectiveId topParent,
      SchedulingActivityDirectiveId anchorId,
      boolean anchoredToStart) {
    return new SchedulingActivityDirective(
        new SchedulingActivityDirectiveId(uniqueId.getAndIncrement()),
        type,
        startOffset,
        duration,
        parameters,
        topParent,
        anchorId,
        anchoredToStart);
  }

  private static SchedulingActivityDirective of(
      SchedulingActivityDirectiveId id,
      ActivityType type,
      Duration startOffset,
      Duration duration,
      Map<String, SerializedValue> parameters,
      SchedulingActivityDirectiveId topParent,
      SchedulingActivityDirectiveId anchorId,
      boolean anchoredToStart) {
    return new SchedulingActivityDirective(
        id, type, startOffset, duration, parameters, topParent, anchorId, anchoredToStart);
  }

  public static SchedulingActivityDirective copyOf(
      SchedulingActivityDirective activityInstance, Duration duration) {
    return SchedulingActivityDirective.of(
        activityInstance.id,
        activityInstance.type,
        activityInstance.startOffset,
        duration,
        new HashMap<>(activityInstance.arguments),
        activityInstance.topParent,
        activityInstance.anchorId,
        activityInstance.anchoredToStart);
  }

  /**
   * Scheduler Activity Directives generated from the Plan have their ID set to the negative of the ActivityDirectiveId
   */
  public static SchedulingActivityDirective fromActivityDirective(
      ActivityDirectiveId id, ActivityDirective activity, ActivityType type, Duration duration) {
    return SchedulingActivityDirective.of(
        new SchedulingActivityDirectiveId(-id.id()),
        type,
        activity.startOffset(),
        duration,
        activity.serializedActivity().getArguments(),
        null,
        (activity.anchorId() != null
            ? new SchedulingActivityDirectiveId(-activity.anchorId().id())
            : null),
        activity.anchoredToStart());
  }

  /**
   * create an activity instance based on the provided one (but a different id)
   *
   * @param o IN the activity instance to copy from
   */
  public static SchedulingActivityDirective of(SchedulingActivityDirective o) {
    return new SchedulingActivityDirective(
        new SchedulingActivityDirectiveId(uniqueId.getAndIncrement()),
        o.type,
        o.startOffset,
        o.duration,
        Map.copyOf(o.arguments),
        o.topParent,
        o.anchorId,
        o.anchoredToStart);
  }

  /**
   * Returns the id of parent activity if this activity is generated.
   */
  public Optional<SchedulingActivityDirectiveId> getParentActivity() {
    return Optional.ofNullable(topParent);
  }

  public Duration getEndTime() {
    return startOffset.plus(duration);
  }

  /**
   * fetches the activity with the earliest end time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithEarliestEndTime(
      List<SchedulingActivityDirective> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::getEndTime));

      return acts.get(0);
    }
    return null;
  }

  /**
   * fetches the activity with the latest end time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithLatestEndTime(
      List<SchedulingActivityDirective> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::getEndTime));

      return acts.get(acts.size() - 1);
    }
    return null;
  }

  /**
   * fetches the activity with the earliest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithEarliestStartTime(
      List<SchedulingActivityDirective> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::startOffset));

      return acts.get(0);
    }
    return null;
  }

  /**
   * fetches the activity with the latest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithLatestStartTime(
      List<SchedulingActivityDirective> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::startOffset));

      return acts.get(acts.size() - 1);
    }
    return null;
  }

  /**
   * fetches the human-legible identifier of the activity instance
   *
   * @return a human-legible identifier for this activity instance
   */
  public SchedulingActivityDirectiveId getId() {
    return this.id;
  }

  /**
   * fetches the activity type specification that this instance is based on
   *
   * @return the activity type specification that this instance is based on
   */
  public ActivityType getType() {
    return type;
  }

  public String toString() {
    return "["
        + this.type.getName()
        + ","
        + this.id
        + ","
        + startOffset
        + ","
        + ((duration != null) ? getEndTime() : "no duration")
        + ", "
        + anchorId
        + ", "
        + anchoredToStart
        + "]";
  }

  /**
   * Checks equality but not in name
   * @param that the other activity instance to compare to
   * @return true if they are equal in properties, false otherwise
   */
  public boolean equalsInProperties(final SchedulingActivityDirective that) {
    return type.equals(that.type)
        && duration.isEqualTo(that.duration)
        && startOffset.isEqualTo(that.startOffset)
        && arguments.equals(that.arguments)
        && Objects.equals(anchorId, that.anchorId)
        && (anchoredToStart == that.anchoredToStart);
  }

  public static Map<String, SerializedValue> instantiateArguments(
      final Map<String, ProfileExpression<?>> arguments,
      final Duration startTime,
      final SimulationResults simulationResults,
      final EvaluationEnvironment environment,
      final ActivityType activityType) {
    final var results = new HashMap<String, SerializedValue>();
    arguments.forEach(
        (key, value) ->
            results.put(
                key,
                value
                    .evaluate(
                        simulationResults, Interval.between(startTime, startTime), environment)
                    .valueAt(startTime)
                    .orElseThrow(
                        () ->
                            new Error(
                                "Profile for argument "
                                    + key
                                    + " has no value at time "
                                    + startTime))));
    return results;
  }

  /**
   * adds an argument to the activity instance
   *
   * @param argument specification. must be identical as the one defined in the model
   * @param param value of the argument
   */
  public void addArgument(String argument, SerializedValue param) {
    assert (type.isParamLegal(argument));
    arguments.put(argument, param);
  }
}
