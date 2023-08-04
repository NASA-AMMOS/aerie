package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class UpdateSimulationExtentAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
        insert into simulation_extent (simulation_dataset_id, extent)
        values (?, ?::interval)
        on conflict (simulation_dataset_id)
        do update
          set extent = ?::interval;
        """;

  private final PreparedStatement statement;

  public UpdateSimulationExtentAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long simulationDatasetId, final Duration extent)
  throws SQLException, NoSuchSimulationDatasetException
  {
    this.statement.setLong(1, simulationDatasetId);
    PreparedStatements.setDuration(this.statement, 2, extent);
    PreparedStatements.setDuration(this.statement, 3, extent);

    final var count = this.statement.executeUpdate();
    if (count < 1) throw new NoSuchSimulationDatasetException(simulationDatasetId);
    if (count > 1) throw new Error("More than one row affected by dataset update by primary key. Is the database corrupted?");
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
