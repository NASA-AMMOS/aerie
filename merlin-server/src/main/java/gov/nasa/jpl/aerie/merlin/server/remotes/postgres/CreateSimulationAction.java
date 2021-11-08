package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class CreateSimulationAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into simulation (plan_id, arguments)
    values (?, '{}')
    returning id, revision
    """;

  private final PreparedStatement statement;

  public CreateSimulationAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public SimulationRecord apply(final long planId) throws SQLException, FailedInsertException {
    this.statement.setLong(1, planId);
    final var results = statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("simulation");

    final var simulationId = results.getLong(1);
    final var simulationRevision = results.getLong(2);
    return new SimulationRecord(simulationId, simulationRevision, planId);
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
