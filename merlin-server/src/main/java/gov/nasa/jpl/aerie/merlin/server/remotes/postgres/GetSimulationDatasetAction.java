package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.SimulationStateRecord.Status;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/*package-local*/ final class GetSimulationDatasetAction implements AutoCloseable {
  private static final @Language("Sql") String sql = """
    select
          d.simulation_id,
          d.status,
          d.reason,
          d.canceled,
          d.offset_from_plan_start,
          d.id
      from simulation_dataset as d
      where
        d.dataset_id = ?
    """;

  private final PreparedStatement statement;

  public GetSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<SimulationDatasetRecord> get(
      final long datasetId,
      final Timestamp planStart
  ) throws SQLException {
    this.statement.setLong(1, datasetId);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) return Optional.empty();

      final Status status;
      try {
        status = Status.fromString(results.getString(2));
      } catch (final Status.InvalidSimulationStatusException ex) {
        throw new Error("Simulation Dataset initialized with invalid state.");
      }

      final var simulationId = results.getLong(1);
      final var reason = results.getString(3);
      final var canceled = results.getBoolean(4);
      final var offsetFromPlanStart = PostgresParsers.parseOffset(results, 5, planStart);
      final var simulationDatasetId = results.getLong(6);
      final var state = new SimulationStateRecord(
          status,
          reason);

      return Optional.of(
          new SimulationDatasetRecord(
              simulationId,
              datasetId,
              state,
              canceled,
              offsetFromPlanStart,
              simulationDatasetId));
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
