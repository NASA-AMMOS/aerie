package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class GetConstraintRunsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      cr.constraintId,
      cr.status
    from constraint_run as cr
    where cr.status = 'success'
  """;

  private final PreparedStatement statement;

  public GetConstraintRunsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<ConstraintRunRecord> get() throws SQLException, ConstraintRunRecord.Status.InvalidRequestStatusException {
    try (final var results = this.statement.executeQuery()) {
      final var constraintRuns = new ArrayList<ConstraintRunRecord>();

      while (results.next()) {
        final var constraintId = results.getLong("constraint_id");
        final var status = results.getString("status");

        constraintRuns.add(
            new ConstraintRunRecord(constraintId, ConstraintRunRecord.Status.fromString(status))
        );
      }

      return constraintRuns;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
