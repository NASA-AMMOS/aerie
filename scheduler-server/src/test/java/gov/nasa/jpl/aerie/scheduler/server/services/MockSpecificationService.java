package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.SpecificationLoadException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;

import java.util.Map;
import java.util.Optional;

class MockSpecificationService implements SpecificationService {
  Map<SpecificationId, Specification> specifications;

  MockSpecificationService(final Map<SpecificationId, Specification> specifications) {
    this.specifications = specifications;
  }

  @Override
  public Specification getSpecification(final SpecificationId specificationId)
  throws NoSuchSpecificationException, SpecificationLoadException
  {
    return Optional.ofNullable(specifications.get(specificationId))
                   .orElseThrow(() -> new NoSuchSpecificationException(specificationId));
  }

  @Override
  public RevisionData getSpecificationRevisionData(final SpecificationId specificationId)
  throws NoSuchSpecificationException
  {
    return $ -> RevisionData.MatchResult.success();
  }
}
