package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import javax.json.Json;
import gov.nasa.jpl.aerie.scheduler.server.http.SchedulerParsers;
import gov.nasa.jpl.aerie.scheduler.server.services.ScheduleFailure;
import org.intellij.lang.annotations.Language;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/*package-local*/ final class GetRequestAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
    select
      r.analysis_id,
      r.status,
      r.reason,
      r.canceled,
      r.dataset_id
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

    final RequestRecord.Status status;
    try {
      status = RequestRecord.Status.fromString(resultSet.getString("status"));
    } catch (final RequestRecord.Status.InvalidRequestStatusException ex) {
      throw new Error(
          String.format(
              "Scheduling request for specification with ID %d and revision %d has invalid state %s",
              specificationId,
              specificationRevision,
              ex.invalidStatus));
    }

    final var analysisId = resultSet.getLong("analysis_id");
    final var failureReason$ = PreparedStatements.getFailureReason(resultSet, "reason");
    final var canceled = resultSet.getBoolean("canceled");
    final var datasetId = PreparedStatements.getDatasetId(resultSet, "dataset_id");
    return Optional.of(new RequestRecord(
        specificationId,
        analysisId,
        specificationRevision,
        status,
        failureReason$,
        canceled,
        datasetId));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
