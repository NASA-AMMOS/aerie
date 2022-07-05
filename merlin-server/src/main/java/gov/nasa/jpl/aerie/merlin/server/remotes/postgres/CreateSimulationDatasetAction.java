package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.SimulationStateRecord.Status;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

/*package local*/ final class CreateSimulationDatasetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into simulation_dataset
      (
        simulation_id,
        offset_from_plan_start
      )
    values(?, ?::timestamptz - ?::timestamptz)
    returning
      dataset_id,
      status,
      reason,
      canceled,
      id
    """;

  private final PreparedStatement statement;

  public CreateSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public SimulationDatasetRecord apply(
      final long simulationId,
      final Timestamp planStart,
      final Timestamp simulationStart
  ) throws SQLException {
    final var offsetFromPlanStart = Duration.of(planStart.microsUntil(simulationStart), MICROSECONDS);

    this.statement.setLong(1, simulationId);
    PreparedStatements.setTimestamp(this.statement, 2, simulationStart);
    PreparedStatements.setTimestamp(this.statement, 3, planStart);

    final var results = this.statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("simulation_dataset");
    final Status status;
    try {
      status = Status.fromString(results.getString(2));
    } catch (final Status.InvalidSimulationStatusException ex) {
      throw new Error("Simulation Dataset initialized with invalid state.");
    }

    final var datasetId = results.getLong(1);
    final var reason = results.getString(3);

    final var state = new SimulationStateRecord(
        status,
        reason);
    final var canceled = results.getBoolean(4);

    final var simulationDatasetId = results.getLong(5);

    return new SimulationDatasetRecord(
        simulationId,
        datasetId,
        state,
        canceled,
        offsetFromPlanStart,
        simulationDatasetId
    );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
