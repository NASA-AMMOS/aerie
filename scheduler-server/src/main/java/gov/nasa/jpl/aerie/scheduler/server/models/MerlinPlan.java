package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MerlinPlan {

  private final Map<ActivityInstanceId, MerlinActivityInstance> activities;

  public MerlinPlan(){
    activities = new HashMap<>();
  }

  public void addActivity(final ActivityInstanceId id, final MerlinActivityInstance activity){
    if(activities.containsKey(id)){
      throw new IllegalArgumentException("Activity with same id is already in the plan");
    }
    activities.put(id, activity);
  }

  public Map<ActivityInstanceId, MerlinActivityInstance> getActivitiesById(){
    return Collections.unmodifiableMap(activities);
  }

  public Optional<MerlinActivityInstance> getActivityById(final ActivityInstanceId id){
    return Optional.ofNullable(activities.get(id));
  }

}
