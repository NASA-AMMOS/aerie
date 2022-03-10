package gov.nasa.jpl.aerie.scheduler.server.mocks;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import gov.nasa.jpl.aerie.scheduler.server.remotes.ResultsCellRepository;
import gov.nasa.jpl.aerie.scheduler.server.remotes.SpecificationRepository;

import java.util.Optional;

public record InMemoryResultsCellRepository(SpecificationRepository specificationRepository) implements ResultsCellRepository {
  @Override
  public ResultsProtocol.OwnerRole allocate(final SpecificationId specificationId)
  {
    throw new UnsupportedOperationException(); // TODO stubbed method must be implemented
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final SpecificationId specificationId)
  {
    throw new UnsupportedOperationException(); // TODO stubbed method must be implemented
  }

  @Override
  public void deallocate(final ResultsProtocol.OwnerRole resultsCell)
  {
    throw new UnsupportedOperationException(); // TODO stubbed method must be implemented
  }
}
