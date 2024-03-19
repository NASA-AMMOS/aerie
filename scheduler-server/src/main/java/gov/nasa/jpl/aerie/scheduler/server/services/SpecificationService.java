package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.SpecificationLoadException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.SpecificationRevisionData;

public record SpecificationService(SpecificationRepository specificationRepository) {
  // Queries
  public Specification getSpecification(final SpecificationId specificationId)
  throws NoSuchSpecificationException, SpecificationLoadException
  {
    return specificationRepository.getSpecification(specificationId);
  }

  public SpecificationRevisionData getSpecificationRevisionData(final SpecificationId specificationId)
  throws NoSuchSpecificationException
  {
    return specificationRepository.getSpecificationRevisionData(specificationId);
  }
}
