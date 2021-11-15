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
          s.simulation_revision
      from simulation_dataset as s
      where s.simulation_id = ?
    """;

  private final PreparedStatement statement;

  public GetDatasetsForSimulationAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<DatasetMetadataRecord> get(final SimulationRecord simulation) throws SQLException {
    final var datasets = new ArrayList<DatasetMetadataRecord>();

    this.statement.setLong(1, simulation.id());
    final var resultSet = this.statement.executeQuery();

    while (resultSet.next()) {
      final var datasetId = resultSet.getLong(1);
      final var modelRevision = resultSet.getLong(2);
      final var planRevision = resultSet.getLong(3);
      final var simulationRevision = resultSet.getLong(4);
      datasets.add(new DatasetMetadataRecord(simulation.id(), datasetId, simulationRevision, planRevision, modelRevision));
    }

    return datasets;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
