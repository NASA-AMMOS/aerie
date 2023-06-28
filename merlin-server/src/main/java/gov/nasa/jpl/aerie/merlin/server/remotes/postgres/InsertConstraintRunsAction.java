package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.violationP;

/* package local */ class InsertConstraintRunsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into constraint_run (constraint_id, constraint_definition, simulation_id, status, violations)
    values (?, ?, ?, ?::constraint_status, ?::json)
  """;

  private final PreparedStatement statement;

  public InsertConstraintRunsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(
      Map<Long, Constraint> constraintMap,
      Map<Long, Violation> violations,
      Long simulationId) throws SQLException {
    for (Constraint constraint : constraintMap.values()) {
      statement.setLong(1, constraint.id());
      statement.setString(2, constraint.definition());
      // TODO: Fix the hardcoded sim id
      statement.setLong(3, 1L);
      statement.setString(4, ConstraintRunRecord.Status.SUCCESS.label);
      statement.setString(5, violationP.unparse(violations.get(constraint.id())).toString());

      this.statement.addBatch();
    }

    this.statement.executeBatch();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
