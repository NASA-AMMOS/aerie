package gov.nasa.jpl.aerie.scheduler.server.remotes;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import java.util.Optional;

public interface ResultsCellRepository {
  ResultsProtocol.OwnerRole allocate(SpecificationId specificationId);

  Optional<ResultsProtocol.OwnerRole> claim(SpecificationId specificationId);

  Optional<ResultsProtocol.ReaderRole> lookup(SpecificationId specificationId);

  void deallocate(ResultsProtocol.OwnerRole resultsCell);
}
