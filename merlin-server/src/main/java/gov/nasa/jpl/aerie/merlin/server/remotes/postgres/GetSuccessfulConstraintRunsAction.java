package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.violationP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.getJsonColumn;

final class GetSuccessfulConstraintRunsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      cr.constraint_id,
      cr.status,
      cr.violations
    from constraint_run as cr
    where cr.status = 'resolved'
  """;

  private final PreparedStatement statement;

  public GetSuccessfulConstraintRunsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<ConstraintRunRecord> get() throws SQLException, ConstraintRunRecord.Status.InvalidRequestStatusException {
    try (final var results = this.statement.executeQuery()) {
      final var constraintRuns = new ArrayList<ConstraintRunRecord>();

      while (results.next()) {
        constraintRuns.add(
            new ConstraintRunRecord(
                results.getLong("constraint_id"),
                ConstraintRunRecord.Status.fromString(results.getString("status")),
                getJsonColumn(results, "violations", violationP)
                    .getSuccessOrThrow($ -> new Error("Corrupt violations cannot be parsed: " + $.reason()))
            )
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
