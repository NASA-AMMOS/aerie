package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.scheduler.server.remotes.postgres.PreparedStatements.getDatasetId;

/*package-local*/ final class CreateRequestAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into scheduling_request (specification_id, specification_revision, requested_by)
      values (?, ?, ?)
      returning
        analysis_id,
        status,
        reason,
        canceled,
        dataset_id
    """;

  private final PreparedStatement statement;

  public CreateRequestAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public RequestRecord apply(final SpecificationRecord specification, final String requestedBy) throws SQLException {
    this.statement.setLong(1, specification.id());
    this.statement.setLong(2, specification.revision());
    this.statement.setString(3, requestedBy);

    final var result = this.statement.executeQuery();
    if (!result.next()) throw new FailedInsertException("scheduling_request");

    final RequestRecord.Status status;
    try {
      status = RequestRecord.Status.fromString(result.getString("status"));
    } catch (final RequestRecord.Status.InvalidRequestStatusException ex) {
      throw new Error("Scheduling request initialized with invalid state.");
    }

    final var analysis_id = result.getLong("analysis_id");
    final var failureReason$ = PreparedStatements.getFailureReason(result, "reason");
    final var canceled = result.getBoolean("canceled");
    final var datasetId = getDatasetId(result, "dataset_id");

    return new RequestRecord(
        specification.id(),
        analysis_id,
        specification.revision(),
        status,
        failureReason$,
        canceled,
        datasetId
    );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
