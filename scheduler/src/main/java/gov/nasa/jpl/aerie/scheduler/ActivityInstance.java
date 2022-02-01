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
    this.id = uniqueId.getAndIncrement();
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
    this.id = uniqueId.getAndIncrement();
    this.type = o.type;
    this.startTime = o.startTime;
    this.duration = o.duration;
    this.parameters = o.parameters;
    this.variableParameters = o.variableParameters;
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
    return this.startTime.plus(this.duration);
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
  public Long getId() {
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
           && parameters.equals(that.parameters)
        && variableParameters.equals(variableParameters);
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
           && Objects.equals(parameters,that.parameters)
           && Objects.equals(variableParameters,that.variableParameters);
  }

  public void instantiateVariableParameters(){
    for (var param : variableParameters.entrySet()) {
      if(!isVariableParameterInstantiated(param.getKey())) {
        instantiateVariableParameter(param.getKey());
      }
    }
  }

  boolean isVariableParameterInstantiated(String name){
    if(!variableParameters.containsKey(name)){
      throw new IllegalStateException(name + " is not a variable parameter");
    }
    return parameters.containsKey(name);
  }

  /*
  * Default policy is to query at activity start
  * TODO: kind of defeats the purpose of an expression
  * */
  public void instantiateVariableParameter(String name) {
    instantiateVariableParameter(name, getStartTime());
  }

  public static SerializedValue getValue(VariableParameterComputer computer, Duration time){
    if (computer instanceof QueriableState state) {
      return state.getValueAtTime(time);
    } else if(computer instanceof StateQueryParam state) {
      return state.getValue(null, Window.at(time));
    } else{
      throw new IllegalArgumentException("Variable parameter specification");
    }
  }

  public SerializedValue getInstantiatedParameterValue(String name, Duration time){
    var paramValue = variableParameters.get(name);
    if(paramValue==null){
      throw new IllegalArgumentException("Unknown parameter "+name);
    }
    return getValue(paramValue, time);
  }

  public void instantiateVariableParameter(String name, Duration time){
    addParameter(name, getInstantiatedParameterValue(name, time));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, duration, startTime, parameters, variableParameters);
  }

  /**
   * unique id
   */
  private final Long id;

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
   * adds a parameter to the activity instance
   *
   * @param parameter specification. must be identical as the one defined in the model
   * @param param value of the parameter
   */
  public void addParameter(String parameter, SerializedValue param) {
    assert(type.isParamLegal(parameter));
    parameters.put(parameter, param);
  }

  public void addVariableParameter(String name, VariableParameterComputer variableParameterComputer) {
    assert(type.isParamLegal(name));
    variableParameters.put(name, variableParameterComputer);
  }

  /**
   * gets all the variable parameters of the activity instance
   *
   * @return a name/value map of parameters for this instance
   */
  public Map<String, VariableParameterComputer> getVariableParameters() {
    return variableParameters;
  }


  /**
   * Sets all the parameters of the activity instance
   *
   * @param params a name/value map of parameters
   */
  public void setParameters(Map<String, SerializedValue> params) {
    this.parameters = params;
  }
  /**
   * Sets all the variable parameters of the activity instance
   *
   * @param params a name/value map of parameters
   */
  public void setVariableParameters(Map<String, VariableParameterComputer> params) {
    this.variableParameters = params;
  }

  /**
   * gets all the parameters of the activity instance
   *
   * @return a name/value map of parameters for this instance
   */
  public Map<String, SerializedValue> getParameters() {
    return parameters;
  }

  /**
   * Parameters are stored in a String/Object hashmap.
   */
  private Map<String, SerializedValue> parameters = new HashMap<>();
  /**
   * uninstantiated parameters
   */
  private Map<String, VariableParameterComputer> variableParameters = new HashMap<>();


  private static final AtomicLong uniqueId = new AtomicLong();
}
