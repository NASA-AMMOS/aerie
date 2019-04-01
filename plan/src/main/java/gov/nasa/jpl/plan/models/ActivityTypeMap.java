package gov.nasa.jpl.plan.models;

import gov.nasa.jpl.aerie.schemas.ActivityType;
import java.util.HashMap;
import java.util.Map;

public class ActivityTypeMap {

  private Map<String, ActivityType> activityTypeMap;

  public ActivityTypeMap() {
    this.setActivityTypeMap(new HashMap<>());
  }

  public Map<String, ActivityType> getActivityTypeMap() {
    return activityTypeMap;
  }

  public void setActivityTypeMap(Map<String, ActivityType> activityTypeMap) {
    this.activityTypeMap = activityTypeMap;
  }
}
