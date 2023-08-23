package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.ConstraintResult;
import gov.nasa.jpl.aerie.merlin.server.models.Constraint;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import static gov.nasa.jpl.aerie.constraints.json.ConstraintParsers.constraintResultP;

/* package local */ class InsertConstraintRunsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into constraint_run (constraint_id, constraint_definition, simulation_dataset_id, results)
    values (?, ?, ?, ?::json)
  """;

  private final PreparedStatement statement;

  public InsertConstraintRunsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(
      Map<Long, Constraint> constraintMap,
      Map<Long, ConstraintResult> results,
      Long simulationDatasetId) throws SQLException {
    for (Constraint constraint : constraintMap.values()) {
      statement.setLong(1, constraint.id());
      statement.setString(2, constraint.definition());
      statement.setLong(3, simulationDatasetId);

      if (results.get(constraint.id()) != null) {
        statement.setString(4, constraintResultP.unparse(results.get(constraint.id())).toString());
      } else {
        statement.setString(4, "{}");
      }

      this.statement.addBatch();
    }

    this.statement.executeBatch();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
