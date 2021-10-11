package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.remotes.ResultsCellRepository;
import org.apache.commons.lang3.NotImplementedException;

import javax.sql.DataSource;
import java.util.Optional;

public final class PostgresResultsCellRepository implements ResultsCellRepository {
  private final DataSource dataSource;

  public PostgresResultsCellRepository(final DataSource dataSource) {
    this.dataSource = dataSource;
  }


  @Override
  public ResultsProtocol.OwnerRole allocate(final String planId, final long planRevision) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }

  @Override
  public Optional<ResultsProtocol.ReaderRole> lookup(final String planId, final long planRevision) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }

  @Override
  public void deallocate(final String planId, final long planRevision) {
    throw new NotImplementedException("If this is needed on the Postgres repository then implement it");
  }
}
