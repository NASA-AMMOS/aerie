package gov.nasa.jpl.aerie.scheduler;


import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * descriptor of a specific execution of a mission behavior
 *
 * (similar to aerie/services/.../plan/models/ActivityInstances.java)
 */
public class ActivityInstance {
  /**
   * creates a new unscheduled activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   */
  //TODO: reconsider unscheduled activity instances
  public ActivityInstance(ActivityType type) {
    this.id = new SchedulingActivityInstanceId(uniqueId.getAndIncrement());
    this.type = type;
    //TODO: should guess duration from activity type bounds
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   * @param start IN the time at which the activity is scheduled
   */
  public ActivityInstance(ActivityType type, Duration start) {
    this(type);
    this.startTime = start;
    //TODO: should guess duration from activity type bounds
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param type IN the datatype signature of and behavior descriptor invoked
   *     by this activity instance
   * @param start IN the time at which the activity is scheduled
   * @param duration IN the duration that the activity lasts for
   */
  public ActivityInstance(ActivityType type, Duration start, Duration duration) {
    this(type, start);
    if (duration.isNegative()) {
      throw new RuntimeException("Negative duration");
    }
    this.duration = duration;
  }

  /**
   * create an activity instance based on the provided one (but adifferent id)
   *
   * @param o IN the activity instance to copy from
   */
  public ActivityInstance(ActivityInstance o) {
    this.id = new SchedulingActivityInstanceId(uniqueId.getAndIncrement());
    this.type = o.type;
    this.startTime = o.startTime;
    this.duration = o.duration;
    this.arguments = o.arguments;
    this.variableArguments = o.variableArguments;
    if (duration.isNegative()) {
      throw new RuntimeException("Negative duration");
    }
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
      acts.sort(Comparator.comparing(ActivityInstance::getStartTime));

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
      acts.sort(Comparator.comparing(ActivityInstance::getStartTime));

      return acts.get(acts.size() - 1);
    }
    return null;
  }


  /**
   * fetches the time at which this activity starts, if specified
   *
   * @return the time at which this activity starts, if specified
   */
  public Duration getStartTime() {
    return this.startTime;
  }

  /**
   * sets the time at which this activity starts
   *
   * @param newStartT the time at which this activity starts
   */
  public void setStartTime(Duration newStartT) {
    this.startTime = newStartT;
  }

  /**
   * fetches the duration over which this activity lasts, if specified
   *
   * @return the duration over which this activity lasts, if specified
   */
  public Duration getDuration() {
    return this.duration;
  }

  public Duration getEndTime() {
    if(!hasDuration()){
      throw new IllegalStateException("Cannot compute end time: activity instance does not have a duration yet");
    }
    if(!hasStartTime()){
      throw new IllegalStateException("Cannot compute end time: activity instance does not have a start time yet");
    }
    return this.startTime.plus(this.duration);
  }

  public boolean hasEndTime(){
    return (hasStartTime() && hasDuration());
  }

  public boolean hasDuration(){
    return (this.duration != null);
  }

  public boolean hasStartTime(){
    return (this.startTime != null);
  }

  /**
   * sets the duration over which this activity lasts
   *
   * @param newDur the new duration to use
   */
  public void setDuration(Duration newDur) {
    if (newDur.isNegative()) {
      throw new RuntimeException("Negative duration");
    }
    this.duration = newDur;
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
    return "[" + this.type.getName() + ","+ this.id + "," + this.getStartTime() + "," + this.getEndTime() + "]";
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
           && arguments.equals(that.arguments)
           && variableArguments.equals(variableArguments);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ActivityInstance that = (ActivityInstance) o;
    return Objects.equals(id, that.id)
           && Objects.equals(type,that.type)
           && duration.isEqualTo(that.duration)
           && startTime.isEqualTo(that.startTime)
           && Objects.equals(arguments, that.arguments)
           && Objects.equals(variableArguments, that.variableArguments);
  }

  public void instantiateVariableArguments(){
    for (var arg : variableArguments.entrySet()) {
      if(!isVariableArgumentInstantiated(arg.getKey())) {
        instantiateVariableArgument(arg.getKey());
      }
    }
  }

  boolean isVariableArgumentInstantiated(String name){
    if(!variableArguments.containsKey(name)){
      throw new IllegalStateException(name + " is not a variable argument");
    }
    return arguments.containsKey(name);
  }

  /*
  * Default policy is to query at activity start
  * TODO: kind of defeats the purpose of an expression
  * */
  public void instantiateVariableArgument(String name) {
    instantiateVariableArgument(name, getStartTime());
  }

  public static SerializedValue getValue(VariableArgumentComputer computer, Duration time){
    if (computer instanceof QueriableState state) {
      return state.getValueAtTime(time);
    } else if(computer instanceof StateQueryParam state) {
      return state.getValue(null, Window.at(time));
    } else{
      throw new IllegalArgumentException("Variable argument specification not supported");
    }
  }

  public SerializedValue getInstantiatedArgumentValue(String name, Duration time){
    var argumentValue = variableArguments.get(name);
    if(argumentValue==null){
      throw new IllegalArgumentException("Unknown argument "+ name);
    }
    return getValue(argumentValue, time);
  }

  public void instantiateVariableArgument(String name, Duration time){
    addArgument(name, getInstantiatedArgumentValue(name, time));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, duration, startTime, arguments, variableArguments);
  }

  /**
   * unique id
   */
  private final SchedulingActivityInstanceId id;

  /**
   * the descriptor for the behavior invoked by this activity instance
   */
  private final ActivityType type;

  /**
   * the length of time this activity instances lasts for after its start
   */
  private Duration duration;

  /**
   * the time at which this activity instance is scheduled to start
   */
  private Duration startTime;

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

  public void addVariableArgument(String name, VariableArgumentComputer variableArgumentComputer) {
    assert(type.isParamLegal(name));
    variableArguments.put(name, variableArgumentComputer);
  }

  /**
   * gets all the variable arguments of the activity instance
   *
   * @return a name/value map of arguments for this instance
   */
  public Map<String, VariableArgumentComputer> getVariableArguments() {
    return variableArguments;
  }


  /**
   * Sets all the arguments of the activity instance
   *
   * @param arguments a name/value map of arguments
   */
  public void setArguments(Map<String, SerializedValue> arguments) {
    this.arguments = arguments;
  }
  /**
   * Sets all the variable arguments of the activity instance
   *
   * @param variableArguments a name/value map of arguments
   */
  public void setVariableArguments(Map<String, VariableArgumentComputer> variableArguments) {
    this.variableArguments = variableArguments;
  }

  /**
   * gets all the arguments of the activity instance
   *
   * @return a name/value map of arguments for this instance
   */
  public Map<String, SerializedValue> getArguments() {
    return arguments;
  }

  /**
   * arguments are stored in a String/Object hashmap.
   */
  private Map<String, SerializedValue> arguments = new HashMap<>();
  /**
   * uninstantiated arguments
   */
  private Map<String, VariableArgumentComputer> variableArguments = new HashMap<>();


  private static final AtomicLong uniqueId = new AtomicLong();
}
