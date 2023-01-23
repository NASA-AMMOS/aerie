package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MerlinPlan {

  private final Map<ActivityDirectiveId, MerlinActivityInstance> activities;

  public MerlinPlan(){
    activities = new HashMap<>();
  }

  public void addActivity(final ActivityDirectiveId id, final MerlinActivityInstance activity){
    if(activities.containsKey(id)){
      throw new IllegalArgumentException("Activity with same id is already in the plan");
    }
    activities.put(id, activity);
  }

  public Map<ActivityDirectiveId, MerlinActivityInstance> getActivitiesById(){
    return Collections.unmodifiableMap(activities);
  }

  public Optional<MerlinActivityInstance> getActivityById(final ActivityDirectiveId id){
    return Optional.ofNullable(activities.get(id));
  }

}
