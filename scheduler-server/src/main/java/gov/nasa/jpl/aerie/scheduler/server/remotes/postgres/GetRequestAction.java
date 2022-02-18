package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/*package-local*/ final class GetRequestAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
    select
      r.analysis_id,
      r.status,
      r.failure_reason,
      r.canceled
    from scheduling_request as r
    where
      r.specification_id = ? and
      r.specification_revision = ?
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

    final var analysisId = resultSet.getLong("analysis_id");
    final var status = resultSet.getString("status");
    final var failureReason = resultSet.getString("failure_reason");
    final var canceled = resultSet.getBoolean("canceled");

    return Optional.of(new RequestRecord(
        specificationId,
        analysisId,
        specificationRevision,
        status,
        failureReason,
        canceled));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
