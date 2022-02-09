package gov.nasa.jpl.aerie.scheduler.server.remotes;

import java.util.Optional;

import gov.nasa.jpl.aerie.scheduler.server.ResultsProtocol;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;

public interface ResultsCellRepository {
  ResultsProtocol.OwnerRole allocate(SpecificationId planId);

  Optional<ResultsProtocol.ReaderRole> lookup(SpecificationId planId);

  void deallocate(ResultsProtocol.OwnerRole resultsCell);
}
