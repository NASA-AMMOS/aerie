package gov.nasa.jpl.aerie.merlin.server;

import java.util.Optional;

public interface ResultsCellRepository {
  ResultsProtocol.OwnerRole allocate(String planId, long planRevision);

  Optional<ResultsProtocol.ReaderRole> lookup(String planId, long planRevision);

  void deallocate(String planId, long planRevision);
}
