package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;

/**
 * services operations at the intersection of plans and scheduling goals; eg scheduling instances to satisfy goals
 *
 * provides both mutation operations to actively improve a plan's goal satisfaction score (eg by inserting activity
 * instances into the plan) and passive queries to ascertain the current satisfaction level of a plan
 */
//TODO: add separate scheduling goal and prioritization management service
public interface SchedulerService {
  /**
   * schedules activity instances into the target plan in order to further satisfy the associated scheduling goals
   *
   * @param request details of the scheduling request, including the target plan and goals to operate on
   * @return summary of the scheduling run, including goal satisfaction metrics and changes made
   */
  ResultsProtocol.State getScheduleResults(final ScheduleRequest request);
}
