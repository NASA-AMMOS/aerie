package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/*package-local*/ final class GetSimulationDatasetAction implements AutoCloseable {
  private static final @Language("Sql") String sql = """
    select
          d.dataset_id,
          d.state,
          d.reason,
          d.canceled,
          d.offset_from_plan_start
      from simulation_dataset as d
      where
        d.simulation_id = ? and
        d.simulation_revision = ? and
        d.plan_revision = ? and
        d.model_revision = ?
    """;

  private final PreparedStatement statement;

  public GetSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<SimulationDatasetRecord> get(
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision,
      final Timestamp planStart
  ) throws SQLException {
    this.statement.setLong(1, simulationId);
    this.statement.setLong(2, simulationRevision);
    this.statement.setLong(3, planRevision);
    this.statement.setLong(4, modelRevision);

    final var results = this.statement.executeQuery();
    if (!results.next()) return Optional.empty();

    final var datasetId = results.getLong(1);
    final var state = new SimulationStateRecord(
        results.getString(2),
        results.getString(3));
    final var canceled = results.getBoolean(4);
    final var offsetFromPlanStart = PostgresParsers.parseOffset(results, 5, planStart);

    return Optional.of(
        new SimulationDatasetRecord(
            simulationId,
            datasetId,
            simulationRevision,
            planRevision,
            modelRevision,
            state,
            canceled,
            offsetFromPlanStart));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
