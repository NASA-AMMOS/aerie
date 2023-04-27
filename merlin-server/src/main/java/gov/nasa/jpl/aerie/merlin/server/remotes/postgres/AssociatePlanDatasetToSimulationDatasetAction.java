package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

/*package-local*/ final class AssociatePlanDatasetToSimulationDatasetAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      insert into plan_dataset_to_simulation_dataset (plan_id, dataset_id, simulation_dataset_id)
      values (?, ?, ?)
      """;

  private final PreparedStatement statement;

  public AssociatePlanDatasetToSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(
      final long planId,
      final long datasetId,
      final long simulationDatasetId
  ) throws SQLException {
    this.statement.setLong(1, planId);
    this.statement.setLong(2, datasetId);
    this.statement.setLong(3, simulationDatasetId);

    final var affectedRows = this.statement.executeUpdate();
    if (affectedRows != 1) throw new FailedInsertException("plan_dataset_to_simulation_dataset");
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
