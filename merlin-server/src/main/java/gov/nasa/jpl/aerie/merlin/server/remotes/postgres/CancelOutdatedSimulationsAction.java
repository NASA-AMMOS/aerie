package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/*package local*/ final class CancelOutdatedSimulationsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    update simulation_dataset as d
    set canceled = true
    from simulation as s
    where
      s.id = d.simulation_id
      and s.revision != d.simulation_revision
      and d.simulation_id = ?
    """;

  private final PreparedStatement statement;

  public CancelOutdatedSimulationsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long simulationId) throws SQLException {
    this.statement.setLong(1, simulationId);
    this.statement.execute();
  }

  @Override
  public void close() throws SQLException {
    statement.close();
  }
}
