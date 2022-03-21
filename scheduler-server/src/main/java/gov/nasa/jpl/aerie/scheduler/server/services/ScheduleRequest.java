package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;

import java.util.Objects;

/**
 * details of a scheduling request, including the target schedule specification version and goals to operate on
 *
 * @param specificationId target schedule specification to read as schedule configuration
 * @param specificationRev the revision of the schedule specification when the schedule request was placed (to determine if stale)
 */
public record ScheduleRequest(SpecificationId specificationId, RevisionData specificationRev) {
  public ScheduleRequest {
    Objects.requireNonNull(specificationId, "specificationId must not be null");
    Objects.requireNonNull(specificationRev, "specificationRev must not be null");
  }
}
