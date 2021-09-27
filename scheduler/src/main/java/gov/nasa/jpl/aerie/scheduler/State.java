package gov.nasa.jpl.aerie.scheduler;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.util.*;

/**
 * the time history of a tracked value
 *
 * States adhere to a StateDefinition the specifies their datatype,
 * legal bounds, and relationships to other kinds of States
 */
public abstract class State<T extends Comparable<T>> {

  private Time horizonStart;
  private Time horizonEnd;
  SortedMap<Range<Time>, T> container;
  Map<ActivityType, Effector<T>> effectors;
  //activities sorted by start time
  Multimap<Time, ActivityInstance> acts;

  /**
   * create a new state based on the given definition
   *
   * @param def IN the definition that this state adheres to
   */
  public State( StateDefinition<T> def ) {
    if( def == null ) { throw new IllegalArgumentException(
        "creating state with null definition" ); }
    this.definition = def;
    this.container = new TreeMap<>();
  }

  public State(Time horizonStart, Time horizonEnd) {
    this.acts = MultimapBuilder.treeKeys().linkedListValues().build();
    this.effectors = new HashMap<ActivityType,Effector<T>>();
    this.container = new TreeMap<>();
    this.horizonStart= horizonStart;
    this.horizonEnd = horizonEnd;
  }



  public void addEffector(Effector<T> eff){
    effectors.put(eff.actType,eff);
  }

  public void addActivityInstance(ActivityInstance act){
    if(!acts.containsValue(act)){
      acts.put(act.getStartTime(), act);
      for(var eff : effectors.values()){
        if(eff.actType == act.getType()){
          this.propagate(act.getStartTime());
          break;
        }
      }
    }
  }


  public T getValueAt(Time t){
    for(var timeAndValue: container.entrySet()){
      if(timeAndValue.getKey().contains(t)&&timeAndValue.getKey().getMaximum().compareTo(t) != 0){
        return timeAndValue.getValue();
      }
    }
    return null;
  }

  public void setValue(T value, Range<Time> interval){
    List<Range<Time>> toRemove = new ArrayList<Range<Time>>();
    Map<Range<Time>, T> toAdd = new HashMap<Range<Time>, T>();

    //cut all other interval
    for(var inter : this.container.entrySet()){

      if(inter.getKey().isAfter(interval)){
        break;
      }
      if(inter.getKey().isBefore(interval)){
        continue;
      }

      var sub = inter.getKey().subtract(interval);
      if(!sub.isEmpty()){
        for(var su : sub) {
          toAdd.put(su, inter.getValue());
        }
      }
      toRemove.add(inter.getKey());
    }
    toAdd.put(interval, value);
    for(var removed : toRemove){
      container.remove(removed);
    }
    for(var added : toAdd.entrySet()){
      container.put(added.getKey(), added.getValue());
    }

  }


  public String toString(){
    return container.toString();
  }

  public abstract T plus(T value1, T value2);

  public abstract T minus(T value1, T value2);
  public abstract T zero();


  private void propagate(Time start){

    HashMap<ActivityInstance,Effector<T>> currentEff = new HashMap<ActivityInstance,Effector<T>>();

    boolean noChanges = false;
    //look for effectors in middle of activities
    for(var act : acts.values()){
      if(act.getStartTime().compareTo(start) < 0 && act.getEndTime().compareTo(start) >0 ){
        currentEff.put(act, effectors.get(act.getType()));
      }
    }

    for(Time t = start; t.compareTo(horizonEnd) <=0; t = t.plus(new Duration(1))){
      //is there new effectors ?
      Collection<ActivityInstance> actStarting = acts.get(t);
      for(var act : actStarting){
        currentEff.put( act,effectors.get(act.getType()));
      }

      //is there effectors that have ended ?
      Time finalT = t;
      currentEff.keySet().removeIf(act -> act.getEndTime().compareTo(finalT) == 0);

      var oldValue = this.getValueAt(t);
      if(oldValue!= null) {
        //this.setValue(0., new Range<Time>(t,t.plus(new Duration(1))));
        // propagate for this timestep
        T val = zero();
        setValue(val,new Range<Time>(t,t.plus(new Duration(1))));
        for (var actEff : currentEff.values()) {
          val = this.plus(val, actEff.getEffectAtTime(t));
        }
        setValue(val,new Range<Time>(t,t.plus(new Duration(1))));

        //has there been a change of value ? if no end loop
        if (oldValue.compareTo(val) == 0) {
          break;
        }
      }
    }


  }


  /**
   * the definition that this state adheres to
   */
  StateDefinition<T> definition;


}
