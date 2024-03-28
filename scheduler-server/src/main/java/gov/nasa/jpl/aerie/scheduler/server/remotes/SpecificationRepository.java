package gov.nasa.jpl.aerie.scheduler.server.remotes;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.SpecificationLoadException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.SpecificationRevisionData;

public interface SpecificationRepository {
  // Queries
  Specification getSpecification(SpecificationId specificationId)
  throws NoSuchSpecificationException, SpecificationLoadException;
  SpecificationRevisionData getSpecificationRevisionData(SpecificationId specificationId) throws NoSuchSpecificationException;
}
