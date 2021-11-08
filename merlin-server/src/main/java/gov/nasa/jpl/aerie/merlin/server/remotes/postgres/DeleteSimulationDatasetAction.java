package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package local*/ final class DeleteSimulationDatasetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    delete from simulation_dataset
    where simulation_id = ?
    and model_revision = ?
    and plan_revision = ?
    and simulation_revision = ?
    """;

  private final PreparedStatement statement;

  public DeleteSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public boolean apply(
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision) throws SQLException {
    this.statement.setLong(1, simulationId);
    this.statement.setLong(2, modelRevision);
    this.statement.setLong(3, planRevision);
    this.statement.setLong(4, simulationRevision);
    return this.statement.execute();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
