package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.SpecificationLoadException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;

public interface SpecificationService {
  // Queries
  Specification getSpecification(SpecificationId specificationId) throws NoSuchSpecificationException, SpecificationLoadException;
  RevisionData getSpecificationRevisionData(SpecificationId specificationId) throws NoSuchSpecificationException;
}
