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

/**
 * @param id unique id
 * @param type the descriptor for the behavior invoked by this activity instance
 * @param startOffset the time at which this activity instance is scheduled to start
 * @param duration the length of time this activity instances lasts for after its start
 * @param arguments arguments are stored in a String/SerializedValue hashmap.
 * @param topParent the parent activity if any
 * @param isNew whether this activity was created in this scheduling run, or already existed in the plan
 */
public record SchedulingActivityDirective(
    ActivityDirectiveId id,
    ActivityType type,
    Duration startOffset,
    Duration duration,
    Map<String, SerializedValue> arguments,
    ActivityDirectiveId topParent,
    ActivityDirectiveId anchorId,
    boolean anchoredToStart,
    boolean isNew
) {

  public static SchedulingActivityDirective of(
      ActivityDirectiveId id,
      ActivityType type,
      Duration startOffset,
      Duration duration,
      ActivityDirectiveId anchorId,
      boolean anchoredToStart,
      boolean isNew
  ) {
    return new SchedulingActivityDirective(
        id,
        type,
        startOffset,
        duration,
        Map.of(),
        null,
        anchorId,
        anchoredToStart,
        isNew
    );
  }

  public static SchedulingActivityDirective of(
      ActivityDirectiveId id,
      ActivityType type,
      Duration startOffset,
      Duration duration,
      Map<String, SerializedValue> parameters,
      ActivityDirectiveId topParent,
      ActivityDirectiveId anchorId,
      boolean anchoredToStart,
      boolean isNew
  ) {
    return new SchedulingActivityDirective(
        id,
        type,
        startOffset,
        duration,
        parameters,
        topParent,
        anchorId,
        anchoredToStart,
        isNew
    );
  }

  public SchedulingActivityDirective withNewDuration(Duration duration){
    return SchedulingActivityDirective.of(
        this.id,
        this.type,
        this.startOffset,
        duration,
        new HashMap<>(this.arguments),
        this.topParent,
        this.anchorId,
        this.anchoredToStart,
        this.isNew()
    );
  }

  public SchedulingActivityDirective withNewAnchor(ActivityDirectiveId anchorId, boolean anchoredToStart, Duration startOffset) {
    return SchedulingActivityDirective.of(
        this.id,
        this.type,
        startOffset,
        this.duration,
        new HashMap<>(this.arguments),
        this.topParent,
        anchorId,
        anchoredToStart,
        this.isNew
    );
  }

  public static SchedulingActivityDirective fromExistingActivityDirective(ActivityDirectiveId id, ActivityDirective activity, ActivityType type, Duration duration){
    return SchedulingActivityDirective.of(
        id,
        type,
        activity.startOffset(),
        duration,
        activity.serializedActivity().getArguments(),
        null,
        activity.anchorId(),
        activity.anchoredToStart(),
        false
    );
  }

  /**
   * Returns the id of parent activity if this activity is generated.
   */
  public Optional<ActivityDirectiveId> getParentActivity(){
    return Optional.ofNullable(topParent);
  }

  public Duration getEndTime(){
    return startOffset.plus(duration);
  }

  /**
   * fetches the activity with the earliest end time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithEarliestEndTime(List<SchedulingActivityDirective> acts) {
    if (!acts.isEmpty()) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::getEndTime));

      return acts.getFirst();
    }
    return null;
  }

  /**
   * fetches the activity with the latest end time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithLatestEndTime(List<SchedulingActivityDirective> acts) {
    if (!acts.isEmpty()) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::getEndTime));

      return acts.getLast();
    }
    return null;
  }

  /**
   * fetches the activity with the earliest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithEarliestStartTime(List<SchedulingActivityDirective> acts) {
    if (!acts.isEmpty()) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::startOffset));

      return acts.getFirst();
    }
    return null;
  }

  /**
   * fetches the activity with the latest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static SchedulingActivityDirective getActWithLatestStartTime(List<SchedulingActivityDirective> acts) {
    if (!acts.isEmpty()) {
      acts.sort(Comparator.comparing(SchedulingActivityDirective::startOffset));

      return acts.getLast();
    }
    return null;
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
    return "[" + this.type.getName() + ","+ this.id + "," + startOffset + "," + ((duration != null) ? getEndTime() : "no duration") + ", "+ topParent + ", " + anchorId+", "+anchoredToStart+"]";
  }

  /**
   * Checks equality but not in name
   * @param that the other activity instance to compare to
   * @return true if they are equal in properties, false otherwise
   */
  public boolean equalsInProperties(final SchedulingActivityDirective that){
    return type.equals(that.type)
           && duration.isEqualTo(that.duration)
           && startOffset.isEqualTo(that.startOffset)
           && arguments.equals(that.arguments)
           && Objects.equals(topParent, that.topParent)
           && Objects.equals(anchorId, that.anchorId)
           && (anchoredToStart == that.anchoredToStart);
  }

  public static Map<String, SerializedValue> instantiateArguments(final Map<String, ProfileExpression<?>> arguments,
                                                                   final Duration startTime,
                                                                   final SimulationResults simulationResults,
                                                                   final EvaluationEnvironment environment,
                                                                  final ActivityType activityType){
    final var results = new HashMap<String, SerializedValue>();
    arguments.forEach((key, value) ->
                          results.put(key,
                                      value.evaluate(simulationResults, Interval.between(startTime, startTime), environment)
                                           .valueAt(startTime)
                                           .orElseThrow(() -> new Error("Profile for argument " + key + " has no value at time " + startTime)))
    );
    return results;
  }

  /**
   * adds an argument to the activity instance
   *
   * @param argument specification. must be identical as the one defined in the model
   * @param param value of the argument
   */
  public void addArgument(String argument, SerializedValue param) {
    assert(type.isParamLegal(argument));
    arguments.put(argument, param);
  }
}
