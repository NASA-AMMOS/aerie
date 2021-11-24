package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol.State;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
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
        model_revision,
        plan_revision,
        simulation_revision,
        state,
        reason,
        canceled,
        offset_from_plan_start
      )
    values(?, ?, ?, ?, ?, ?, ?, ?::timestamptz - ?::timestamptz)
    returning dataset_id
    """;

  private final PreparedStatement statement;

  public CreateSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public SimulationDatasetRecord apply(
      final long simulationId,
      final long modelRevision,
      final long planRevision,
      final long simulationRevision,
      final Timestamp planStart,
      final Timestamp simulationStart,
      final State simulationState
  ) throws SQLException {
    final var state = SimulationStateRecord.fromSimulationState(simulationState);
    final var canceled = false;
    final var offsetFromPlanStart = Duration.of(planStart.microsUntil(simulationStart), MICROSECONDS);

    this.statement.setLong(1, simulationId);
    this.statement.setLong(2, modelRevision);
    this.statement.setLong(3, planRevision);
    this.statement.setLong(4, simulationRevision);
    this.statement.setString(5, state.state());
    this.statement.setString(6, state.reason());
    this.statement.setBoolean(7, canceled);
    PreparedStatements.setTimestamp(this.statement, 8, simulationStart);
    PreparedStatements.setTimestamp(this.statement, 9, planStart);

    final var results = this.statement.executeQuery();
    if (!results.next()) throw new FailedInsertException("dataset");
    final var datasetId = results.getLong(1);

    return new SimulationDatasetRecord(
        simulationId,
        datasetId,
        simulationRevision,
        modelRevision,
        planRevision,
        state,
        canceled,
        offsetFromPlanStart
    );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
