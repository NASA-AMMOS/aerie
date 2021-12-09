package gov.nasa.jpl.aerie.scheduler.server.services;

import java.util.Objects;

/**
 * details of a scheduling request, including the target plan version and goals to operate on
 *
 * @param planId target plan to read as initial input schedule as well as target for the output schedule
 * @param planRev the revision of the plan when the schedule request was placed (to determine if stale)
 */
public record ScheduleRequest(String planId, long planRev) {
  public ScheduleRequest {
    Objects.requireNonNull(planId, "planId must not be null");
    Objects.requireNonNull(planRev, "planRev must not be null");
  }
}
