package gov.nasa.jpl.aerie.scheduler.server.services;

import java.util.Map;

/**
 * summary of results from running the scheduler, including goal satisfaction metrics and changes made
 *
 * @param activityCount number of distinct activity instances in the plan (counting parents/children separately)
 * @param goalScores the satisfaction scores for each goal considered, indexed by the goal name. duplicated goal
 *     names (or goals evaluated under different criteria) will have scores overwritten in an unspecified order
 */
public record ScheduleResults(long activityCount, Map<String, Double> goalScores) {

}
