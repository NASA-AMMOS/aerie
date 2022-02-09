package gov.nasa.jpl.aerie.scheduler.server.remotes;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;

public interface SpecificationRepository {
  // Queries
  Specification getSpecification(SpecificationId specificationId) throws NoSuchSpecificationException;
  RevisionData getSpecificationRevisionData(SpecificationId specificationId) throws NoSuchSpecificationException;
}
