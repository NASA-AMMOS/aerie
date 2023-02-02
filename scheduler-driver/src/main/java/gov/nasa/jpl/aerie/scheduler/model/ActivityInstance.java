package gov.nasa.jpl.aerie.scheduler.model;


import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.tree.ProfileExpression;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * descriptor of a specific execution of a mission behavior
 *
 * (similar to aerie/services/.../plan/models/ActivityInstances.java)
 */

/**
 * @param id unique id
 * @param type the descriptor for the behavior invoked by this activity instance
 * @param startTime the time at which this activity instance is scheduled to start
 * @param duration the length of time this activity instances lasts for after its start
 * @param arguments arguments are stored in a String/SerializedValue hashmap.
 * @param topParent the parent activity if any
 */
public record ActivityInstance(
    SchedulingActivityInstanceId id,
    ActivityType type,
    Duration startTime,
    Duration duration,
    Map<String, SerializedValue> arguments,
    SchedulingActivityInstanceId topParent
    ){

  private static final AtomicLong uniqueId = new AtomicLong();


  /**
   * creates a new unscheduled activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   */
  //TODO: reconsider unscheduled activity instances
  public static ActivityInstance of(ActivityType type) {
    return new ActivityInstance(new SchedulingActivityInstanceId(uniqueId.getAndIncrement()), type,
                                Duration.ZERO,
                                Duration.ZERO,
                                Map.of(), null);
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   * @param start IN the time at which the activity is scheduled
   */
  public static ActivityInstance of(ActivityType type, Duration start) {
    return new ActivityInstance(new SchedulingActivityInstanceId(uniqueId.getAndIncrement()), type,
                                start,
                                Duration.ZERO,
                                Map.of(), null);
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   * @param start IN the time at which the activity is scheduled
   * @param duration IN the duration that the activity lasts for
   */
  public static ActivityInstance of(ActivityType type, Duration start, Duration duration) {
    return new ActivityInstance(new SchedulingActivityInstanceId(uniqueId.getAndIncrement()), type,
                                start,
                                duration,
                                Map.of(), null);

  }

  public static ActivityInstance of(ActivityType type, Duration start, Duration duration, Map<String, SerializedValue> parameters) {
    return new ActivityInstance(new SchedulingActivityInstanceId(uniqueId.getAndIncrement()), type,
                                start,
                                duration,
                                parameters, null);
  }

  public static ActivityInstance of(ActivityType type, Duration start, Duration duration, Map<String, SerializedValue> parameters, SchedulingActivityInstanceId topParent) {
    return new ActivityInstance(new SchedulingActivityInstanceId(uniqueId.getAndIncrement()), type,
                                start,
                                duration,
                                parameters, topParent);
  }

  private static ActivityInstance of(SchedulingActivityInstanceId id, ActivityType type, Duration start, Duration duration, Map<String, SerializedValue> parameters, SchedulingActivityInstanceId topParent) {
    return new ActivityInstance(id,
                                type,
                                start,
                                duration,
                                parameters,
                                topParent);
  }

  public static ActivityInstance copyOf(ActivityInstance activityInstance, Duration duration){
    return ActivityInstance.of(activityInstance.id,
            activityInstance.type,
            activityInstance.startTime,
            duration,
            new HashMap<>(activityInstance.arguments),
            activityInstance.topParent);
  }

  /**
   * create an activity instance based on the provided one (but adifferent id)
   *
   * @param o IN the activity instance to copy from
   */
  public static ActivityInstance of(ActivityInstance o) {
    return new ActivityInstance(
        new SchedulingActivityInstanceId(uniqueId.getAndIncrement()),
        o.type,
        o.startTime, o.duration,
        o.arguments,
        o.topParent
    );
  }

  /**
   * Returns the id of parent activity if this activity is generated.
   */
  public Optional<SchedulingActivityInstanceId> getParentActivity(){
    return Optional.ofNullable(topParent);
  }

  public Duration getEndTime(){
    return startTime.plus(duration);
  }

  /**
   * fetches the activity with the earliest end time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithEarliestEndTtime(List<ActivityInstance> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(ActivityInstance::getEndTime));

      return acts.get(0);
    }
    return null;
  }

  /**
   * fetches the activity with the latest end time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithLatestEndTtime(List<ActivityInstance> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(ActivityInstance::getEndTime));

      return acts.get(acts.size() - 1);
    }
    return null;
  }

  /**
   * fetches the activity with the earliest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithEarliestStartTtime(List<ActivityInstance> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(ActivityInstance::startTime));

      return acts.get(0);
    }
    return null;
  }

  /**
   * fetches the activity with the latest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithLatestStartTtime(List<ActivityInstance> acts) {
    if (acts.size() > 0) {
      acts.sort(Comparator.comparing(ActivityInstance::startTime));

      return acts.get(acts.size() - 1);
    }
    return null;
  }



  /**
   * fetches the human-legible identifier of the activity instance
   *
   * @return a human-legible identifier for this activity instance
   */
  public SchedulingActivityInstanceId getId() {
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
    return "[" + this.type.getName() + ","+ this.id + "," + startTime + "," + ((duration != null) ? getEndTime() : "no duration") + "]";
  }

  /**
   * Checks equality but not in name
   * @param that the other activity instance to compare to
   * @return true if they are equal in properties, false otherwise
   */
  public boolean equalsInProperties(final ActivityInstance that){
    return type.equals(that.type)
           && duration.isEqualTo(that.duration)
           && startTime.isEqualTo(that.startTime)
           && arguments.equals(that.arguments);
  }

  public static Map<String, SerializedValue> instantiateArguments(final Map<String, ProfileExpression<?>> arguments,
                                                                   final Duration startTime,
                                                                   final SimulationResults simulationResults,
                                                                   final EvaluationEnvironment environment,
                                                                  final ActivityType activityType){
    final var results = new HashMap<String, SerializedValue>();
    arguments.forEach((key, value) ->
                          results.put(key,
                                      value.evaluate(simulationResults, Interval.FOREVER, environment)
                                           .valueAt(startTime)
                                           .orElseThrow(() -> new Error("Profile for argument " + key + " has no value at time " + startTime)))
    );
    return castCorrectToInt(results, activityType);
  }


  public static Map<String, SerializedValue> castCorrectToInt(Map<String, SerializedValue> arguments, ActivityType activityType){
    final var ret = new HashMap<String, SerializedValue>();
    final var parameters = activityType.getSpecType().getInputType().getParameters();
    for(final var arg: arguments.entrySet()){
      for(final var param : parameters) {
       if(param.name().equals(arg.getKey())) {
         ret.put(arg.getKey(),castCorrectly(arg.getValue(), param.schema()));
        break;
       }
      }
    }
    return ret;
  }

  static SerializedValue castCorrectly(final SerializedValue value, final ValueSchema correctValueSchema){
    return value.match(new SerializedValue.Visitor<>() {
      @Override
      public SerializedValue onNull() {
        return SerializedValue.NULL;
      }

      @Override
      public SerializedValue onReal(final double value) {
        if(correctValueSchema.asReal().isPresent()){
          return SerializedValue.of(value);
        } else if(correctValueSchema.asInt().isPresent()){
          return SerializedValue.of((int)value);
        }
        throw new IllegalArgumentException();      }

      @Override
      public SerializedValue onInt(final long value) {
        if(correctValueSchema.asInt().isPresent() || correctValueSchema.asDuration().isPresent()){
          return SerializedValue.of(value);
        } else if(correctValueSchema.asReal().isPresent()){
          return SerializedValue.of((double)(value));
        }
        throw new IllegalArgumentException();      }

      @Override
      public SerializedValue onBoolean(final boolean value) {
        return SerializedValue.of(value);
      }

      @Override
      public SerializedValue onString(final String value) {
        return SerializedValue.of(value);
      }

      @Override
      public SerializedValue onMap(final Map<String, SerializedValue> value) {
        final var lsit = new HashMap<String, SerializedValue>();
        for(final var val : value.entrySet()) {
          lsit.put(val.getKey(), castCorrectly(val.getValue(), correctValueSchema.asStruct().get().get(val.getKey())));
        }
        return SerializedValue.of(lsit);
      }

      @Override
      public SerializedValue onList(final List<SerializedValue> value) {
        final var lsit = new ArrayList<SerializedValue>();
        for(final var val : value) {
          lsit.add(castCorrectly(val, correctValueSchema.asSeries().get()));
        }
        return SerializedValue.of(lsit);
      }

    });
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
