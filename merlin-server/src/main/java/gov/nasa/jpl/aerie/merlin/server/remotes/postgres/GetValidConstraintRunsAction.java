package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.violationP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.getJsonColumn;

final class GetValidConstraintRunsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      cr.constraint_id,
      cr.simulation_dataset_id,
      cr.definition_outdated,
      cr.violations
    from constraint_run as cr
    where cr.definition_outdated = false
    and cr.constraint_id = any(?)
    and cr.simulation_dataset_id in (select distinct on (simulation_id) id from simulation_dataset order by simulation_id, id desc)
  """;

  private final PreparedStatement statement;
  private final List<Long> constraintIds;

  public GetValidConstraintRunsAction(final Connection connection, final List<Long> constraintIds) throws SQLException {
    this.statement = connection.prepareStatement(sql);
    this.constraintIds = constraintIds;
  }

  public List<ConstraintRunRecord> get() throws SQLException {
    this.statement.setArray(1, this.statement.getConnection().createArrayOf("integer", constraintIds.toArray()));

    try (final var results = this.statement.executeQuery()) {
      final var constraintRuns = new ArrayList<ConstraintRunRecord>();

      while (results.next()) {
        final var constraintId = results.getLong("constraint_id");
        final var violationString = results.getString("violations");

        // The constraint run didn't have any violations
        if (violationString.equals("{}")) {
          constraintRuns.add(new ConstraintRunRecord(constraintId, null));
        } else {
          constraintRuns.add(new ConstraintRunRecord(
              constraintId,
              getJsonColumn(results, "violations", violationP)
                  .getSuccessOrThrow($ -> new Error("Corrupt violations cannot be parsed: " + $.reason()))));
        }
      }

      return constraintRuns;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
