package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.constraints.model.Violation;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/* package local */ class InsertConstraintRunsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into constraint_run (constraint_id, constraint_definition, simulation_id, violations)
    values (?, ?, ?, ?)
  """;

  private final PreparedStatement statement;

  public InsertConstraintRunsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(Long constraintId, String constraintDefinition, List<Violation> violations, Long simulationId) {

    this.statement.executeBatch();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
