package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.PostgresSpecificationRepository;

public interface SpecificationService {
  // Queries
  Specification getSpecification(SpecificationId specificationId)
  throws NoSuchSpecificationException, PostgresSpecificationRepository.GoalBuildFailureException;
  RevisionData getSpecificationRevisionData(SpecificationId specificationId) throws NoSuchSpecificationException;
}
