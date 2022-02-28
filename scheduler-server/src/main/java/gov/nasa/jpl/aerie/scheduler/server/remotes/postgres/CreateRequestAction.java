package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class CreateRequestAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into scheduling_request (specification_id, specification_revision)
      values (?, ?)
      returning
        analysis_id,
        status,
        failure_reason,
        canceled
    """;

  private final PreparedStatement statement;

  public CreateRequestAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public RequestRecord apply(final SpecificationRecord specification) throws SQLException {
    this.statement.setLong(1, specification.id());
    this.statement.setLong(2, specification.revision());

    final var result = this.statement.executeQuery();
    if (!result.next()) throw new FailedInsertException("scheduling_request");

    final RequestRecord.Status status;
    try {
      status = RequestRecord.Status.fromString(result.getString("status"));
    } catch (final RequestRecord.Status.InvalidRequestStatusException ex) {
      throw new Error("Scheduling request initialized with invalid state.");
    }

    final var analysis_id = result.getLong("analysis_id");
    final var failureReason = result.getString("failure_reason");
    final var canceled = result.getBoolean("canceled");

    return new RequestRecord(
        specification.id(),
        analysis_id,
        specification.revision(),
        status,
        failureReason,
        canceled
    );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
