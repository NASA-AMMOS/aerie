package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*package local*/ final class GetDatasetsForSimulationAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
          s.dataset_id,
          s.model_revision,
          s.plan_revision,
          s.simulation_revision,
          s.state,
          s.reason,
          s.canceled
      from simulation_dataset as s
      where s.simulation_id = ?
    """;

  private final PreparedStatement statement;

  public GetDatasetsForSimulationAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<SimulationDatasetRecord> get(final SimulationRecord simulation) throws SQLException {
    final var datasets = new ArrayList<SimulationDatasetRecord>();

    this.statement.setLong(1, simulation.id());
    final var resultSet = this.statement.executeQuery();

    while (resultSet.next()) {
      final var datasetId = resultSet.getLong(1);
      final var modelRevision = resultSet.getLong(2);
      final var planRevision = resultSet.getLong(3);
      final var simulationRevision = resultSet.getLong(4);
      final var state = resultSet.getString(5);
      final var reason = resultSet.getString(6);
      final var canceled = resultSet.getBoolean(7);
      datasets.add(new SimulationDatasetRecord(
          simulation.id(),
          datasetId,
          simulationRevision,
          planRevision,
          modelRevision,
          state,
          reason,
          canceled));
    }

    return datasets;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
