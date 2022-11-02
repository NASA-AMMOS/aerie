package gov.nasa.jpl.aerie.scheduler.server.services;

import java.util.Collection;
import java.util.Map;
import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;

/**
 * summary of results from running the scheduler, including goal satisfaction metrics and changes made
 * TODO: @param javadocs (Adrien)
 */
public record ScheduleResults(Map<GoalId, GoalResult> goalResults) {

  public record GoalResult(
      Collection<ActivityInstanceId> createdActivities,
      Collection<ActivityInstanceId> satisfyingActivities,
      boolean satisfied
  )
  { }
}
