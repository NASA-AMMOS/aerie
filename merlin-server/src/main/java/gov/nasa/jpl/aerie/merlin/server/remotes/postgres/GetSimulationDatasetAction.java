package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.SimulationStateRecord.Status;
import gov.nasa.jpl.aerie.types.Timestamp;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;

public final class GetSimulationDatasetAction implements AutoCloseable {
  private static final @Language("Sql") String sql = """
    select
          d.simulation_id as simulation_id,
          d.status as status,
          d.reason as reason,
          d.canceled as canceled,
          to_char(d.simulation_start_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as simulation_start_time,
          to_char(d.simulation_end_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as simulation_end_time,
          d.id as id,
          d.arguments
      from merlin.simulation_dataset as d
      where
        d.dataset_id = ?
    """;

  private final PreparedStatement statement;

  public GetSimulationDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<SimulationDatasetRecord> get(
      final long datasetId
  ) throws SQLException {
    this.statement.setLong(1, datasetId);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) return Optional.empty();

      final Status status;
      try {
        status = Status.fromString(results.getString("status"));
      } catch (final Status.InvalidSimulationStatusException ex) {
        throw new Error("Simulation Dataset initialized with invalid state.");
      }

      final var simulationId = results.getLong("simulation_id");
      final var reason = PreparedStatements.getFailureReason(results, 3);
      final var canceled = results.getBoolean("canceled");
      final var simulationDatasetId = results.getLong("id");
      final var state = new SimulationStateRecord(status, reason);
      final var simStartTime = Timestamp.fromString(results.getString("simulation_start_time"));
      final var simEndTime = Timestamp.fromString(results.getString("simulation_end_time"));
      final var arguments = parseSerializedValue(results.getString("arguments")).asMap().get();

      return Optional.of(
          new SimulationDatasetRecord(
              simulationId,
              datasetId,
              state,
              canceled,
              simStartTime,
              simEndTime,
              simulationDatasetId,
              arguments
          ));
    }
  }

  private static SerializedValue parseSerializedValue(final String value) {
    final SerializedValue serializedValue;
    try (
        final var serializedValueReader = Json.createReader(new StringReader(value))
    ) {
      serializedValue = serializedValueP
          .parse(serializedValueReader.readValue())
          .getSuccessOrThrow();
    }
    return serializedValue;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
