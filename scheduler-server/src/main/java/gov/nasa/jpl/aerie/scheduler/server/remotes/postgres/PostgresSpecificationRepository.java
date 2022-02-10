package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import javax.sql.DataSource;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchSpecificationException;
import gov.nasa.jpl.aerie.scheduler.server.models.Specification;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;
import gov.nasa.jpl.aerie.scheduler.server.services.RevisionData;

public record PostgresSpecificationRepository(DataSource dataSource) implements SpecificationRepository {

  @Override
  public Specification getSpecification(final SpecificationId specificationId) throws NoSuchSpecificationException
  {
    throw new UnsupportedOperationException(); // TODO stubbed method must be implemented
  }

  @Override
  public RevisionData getSpecificationRevisionData(final SpecificationId specificationId)
  throws NoSuchSpecificationException
  {
    throw new UnsupportedOperationException(); // TODO stubbed method must be implemented
  }
}
