package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;

public final class LocalSpecificationService implements SpecificationService {
  @Override
  public Specification getSpecification(final SpecificationId specificationId) throws NoSuchSpecificationException
  {
    // TODO needs to be implemented
    throw new UnsupportedOperationException();
  }

  @Override
  public RevisionData getSpecificationRevisionData(final SpecificationId specificationId)
  throws NoSuchSpecificationException
  {
    // TODO needs to be implemented
    throw new UnsupportedOperationException();
  }
}
