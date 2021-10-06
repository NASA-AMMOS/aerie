package gov.nasa.jpl.aerie.scheduler;

import java.util.*;

/**
 * descriptor of a specific execution of a mission behavior
 *
 * (similar to aerie/services/.../plan/models/ActivityInstances.java)
 */
public class ActivityInstance {

  /**
   * creates a new unscheduled activity instance of specified type
   *
   * @param name IN the human legible name of the activity instance
   * @param type IN the datatype signature of and behavior descriptor invoked
   *        by this activity instance
   */
  //TODO: reconsider unscheduled activity instances
  public ActivityInstance( String name, ActivityType type ) {
    this.name = name;
    this.type = type;
    //TODO: should guess duration from activity type bounds
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param name IN the human legible name of the activity instance
   * @param type IN the datatype signature of and behavior descriptor invoked
   *        by this activity instance
   * @param start IN the time at which the activity is scheduled
   */
  public ActivityInstance(String name, ActivityType type, Time start ) {
    this( name, type );
    this.startTime = start;
    //TODO: should guess duration from activity type bounds
  }

  /**
   * creates a new activity instance of specified type
   *
   * @param name IN the human legible name of the activity instance
   * @param type IN the datatype signature of and behavior descriptor invoked
   *        by this activity instance
   * @param start IN the time at which the activity is scheduled
   * @param duration IN the duration that the activity lasts for
   */
  public ActivityInstance(String name, ActivityType type, Time start, Duration duration ) {
    this( name, type, start );
    if(duration.toMilliseconds() < 0){
      throw new RuntimeException("Negative duration");
    }
    this.duration = duration;
  }

  /**
   * create an activity instance based on the provided one
   *
   * @param o IN the activity instance to copy from
   */
  public ActivityInstance( ActivityInstance o ) {
    this.name = o.name; //TODO: names should probably not be replicated
    this.type = o.type;
    this.startTime = o.startTime;
    this.duration = o.duration;
    this.parameters = o.parameters;

    if(duration.toMilliseconds() < 0){
      throw new RuntimeException("Negative duration");
    }
  }


  /**
   * fetches the activity with the earliest end time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithEarliestEndTtime(List<ActivityInstance> acts){
    if(acts.size()>0) {
      Collections.sort(acts, new Comparator<ActivityInstance>() {
        @Override
        public int compare(ActivityInstance u1, ActivityInstance u2) {
          return u1.getEndTime().compareTo(u2.getEndTime());
        }
      });

      return acts.get(0);
    }
    return null;
  }

  /**
   * fetches the activity with the latest end time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithLatestEndTtime(List<ActivityInstance> acts){
    if(acts.size()>0) {
      Collections.sort(acts, new Comparator<ActivityInstance>() {
        @Override
        public int compare(ActivityInstance u1, ActivityInstance u2) {
          return u1.getEndTime().compareTo(u2.getEndTime());
        }
      });

      return acts.get(acts.size()-1);
    }
    return null;
  }

  /**
   * fetches the activity with the earliest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithEarliestStartTtime(List<ActivityInstance> acts){
    if(acts.size()>0) {
      Collections.sort(acts, new Comparator<ActivityInstance>() {
        @Override
        public int compare(ActivityInstance u1, ActivityInstance u2) {
          return u1.getStartTime().compareTo(u2.getStartTime());
        }
      });

      return acts.get(0);
    }
    return null;
  }

  /**
   * fetches the activity with the latest starting time in a list of activity instances
   *
   * @return the activity
   */
  public static ActivityInstance getActWithLatestStartTtime(List<ActivityInstance> acts){
    if(acts.size()>0) {
      Collections.sort(acts, new Comparator<ActivityInstance>() {
      @Override
      public int compare(ActivityInstance u1, ActivityInstance u2) {
        return u1.getStartTime().compareTo(u2.getStartTime());
      }
    });

    return acts.get(acts.size()-1);
    }
    return null;
  }


  /**
   * fetches the time at which this activity starts, if specified
   *
   * @return the time at which this activity starts, if specified
   */
  public Time getStartTime() {
    return this.startTime;
  }

  /**
   * sets the time at which this activity starts
   * @param newStartT the time at which this activity starts
   */
  public void setStartTime( Time newStartT ) {
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

  public Time getEndTime(){
    return this.startTime.plus(this.duration);
  }

  /**
   * sets the duration over which this activity lasts
   * @param newDur the new duration to use
   */
  public void setDuration( Duration newDur ) {
    if(newDur.toMilliseconds() < 0){
      throw new RuntimeException("Negative duration");
    }
    this.duration = newDur;
  }

  /**
   * fetches the human-legible identifier of the activity instance
   *
   * @return a human-legible identifier for this activity instance
   */
  public String getName() {
    return this.name;
  }

  /**
   * fetches the activity type specification that this instance is based on
   *
   * @return the activity type specification that this instance is based on
   */
  public ActivityType getType() {
    return type;
  }

  public String toString(){
    return "["+this.name+","+this.getStartTime()+","+this.getEndTime()+"]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ActivityInstance that = (ActivityInstance) o;
    return name.equals(that.name) && type.equals(that.type) && duration.equals(that.duration) && startTime.equals(that.startTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, duration, startTime);
  }

  /**
   * the human-legible identifier of the activity instance
   */
  private String name;

  /**
   * the descriptor for the behavior invoked by this activity instance
   */
  private ActivityType type;

  /**
   * the length of time this activity instances lasts for after its start
   */
  private Duration duration;

  /**
   * the time at which this activity instance is scheduled to start
   */
  private Time startTime;

  /**
   * adds a parameter to the activity instance
   * @param name name of the parameter
   * @param param value of the parameter
   */
  public void addParameter(String name, Object param){
    parameters.put(name, param);
  }

  /**
   * Sets all the parameters of the activity instance
   * @param params a name/value map of parameters
   */
  public void setParameters(Map<String, Object> params){
    this.parameters = params;
  }
  /**
   * gets all the parameters of the activity instance
  * @return a name/value map of parameters for this instance
   */
  public Map<String, Object> getParameters(){
    return parameters;
  }

  /**
   * Parameters are stored in a String/Object hashmap.
   */
  private Map<String, Object> parameters = new HashMap<String, Object>();

}
