package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/*package-local*/ final class GetDatasetMetadataAction implements AutoCloseable {
  private static final @Language("Sql") String sql = """
    select
          d.dataset_id
      from simulation_dataset as d
      where
        d.simulation_id = ? and
        d.simulation_revision = ? and
        d.plan_revision = ? and
        d.model_revision = ?
    """;

  private final PreparedStatement statement;

  public GetDatasetMetadataAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<DatasetMetadataRecord> get(
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision
  ) throws SQLException {
    this.statement.setLong(1, simulationId);
    this.statement.setLong(2, simulationRevision);
    this.statement.setLong(3, planRevision);
    this.statement.setLong(4, modelRevision);

    final var results = this.statement.executeQuery();
    if (!results.next()) return Optional.empty();

    final var datasetId = results.getLong(1);

    return Optional.of(new DatasetMetadataRecord(
        simulationId,
        datasetId,
        simulationRevision,
        planRevision,
        modelRevision
    ));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
