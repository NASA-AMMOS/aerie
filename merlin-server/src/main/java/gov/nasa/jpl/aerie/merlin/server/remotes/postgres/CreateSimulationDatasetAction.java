package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package local*/ final class CreateSimulationDatasetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into simulation_dataset
      (
        simulation_id,
        dataset_id,
        model_revision,
        plan_revision,
        simulation_revision,
        state,
        reason,
        canceled
      )
    values(?, ?, ?, ?, ?, ?, ?, ?)
    """;

  private final PreparedStatement statement;

  public CreateSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public boolean apply(
      final long simulationId,
      final long datasetId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision,
      final State simulationState
  ) throws SQLException {
    final var canceled = false;

    this.statement.setLong(1, simulationId);
    this.statement.setLong(2, datasetId);
    this.statement.setLong(3, modelRevision);
    this.statement.setLong(4, planRevision);
    this.statement.setLong(5, simulationRevision);
    PreparedStatements.setSimulationState(this.statement, 6, 7, simulationState);
    this.statement.setBoolean(8, canceled);

    return statement.execute();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
