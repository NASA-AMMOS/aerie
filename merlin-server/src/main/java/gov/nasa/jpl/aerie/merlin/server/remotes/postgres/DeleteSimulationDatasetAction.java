package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package local*/ final class DeleteSimulationDatasetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    delete from simulation_dataset
    where dataset_id = ?
    """;

  private final PreparedStatement statement;

  public DeleteSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public boolean apply(final long datasetId) throws SQLException {
    this.statement.setLong(1, datasetId);
    return this.statement.execute();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
