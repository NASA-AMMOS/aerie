package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class SetSimulationStateAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
        update simulation_dataset
          set
            state = ?,
            reason = ?
          where dataset_id = ?
        """;

  private final PreparedStatement statement;

  public SetSimulationStateAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long datasetId, final State simulationState)
  throws SQLException, NoSuchSimulationDatasetException
  {
    final var state = SimulationStateRecord.fromSimulationState(simulationState);
    this.statement.setString(1, state.status().label);
    this.statement.setString(2, state.reason());
    this.statement.setLong(3, datasetId);

    final var count = this.statement.executeUpdate();
    if (count < 1) throw new NoSuchSimulationDatasetException(datasetId);
    if (count > 1) throw new Error("More than one row affected by dataset update by primary key. Is the database corrupted?");
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
