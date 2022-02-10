package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchRequestException;
import gov.nasa.jpl.aerie.scheduler.server.models.SpecificationId;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/*package-local*/ final class GetRequestAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
    select
      req.analysis_id
    from scheduling_request as req
      where
        req.specification_id = ? and
        req.specification_revision = ?
    """;

  private final PreparedStatement statement;

  public GetRequestAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<RequestRecord> get(
      final long specificationId,
      final long specificationRevision
  ) throws SQLException {
    this.statement.setLong(1, specificationId);
    this.statement.setLong(2, specificationRevision);

    final var resultSet = this.statement.executeQuery();
    if (!resultSet.next()) return Optional.empty();

    final var analysisId = resultSet.getLong(1);

    return Optional.of(new RequestRecord(
        specificationId,
        analysisId,
        specificationRevision));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
