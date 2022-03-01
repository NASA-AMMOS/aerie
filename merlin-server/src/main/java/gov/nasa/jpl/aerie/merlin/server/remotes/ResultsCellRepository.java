package gov.nasa.jpl.aerie.merlin.server.remotes;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import java.util.Optional;

public interface ResultsCellRepository {
  ResultsProtocol.OwnerRole allocate(PlanId planId);

  Optional<ResultsProtocol.OwnerRole> claim(PlanId planId, Long datasetId);

  Optional<ResultsProtocol.ReaderRole> lookup(PlanId planId);

  void deallocate(ResultsProtocol.OwnerRole resultsCell);
}
