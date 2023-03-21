package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.getJsonColumn;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.simulationArgumentsP;

/*package local*/ final class GetSimulationTemplateAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
          t.model_id,
          t.revision,
          t.description,
          t.arguments,
          to_char(t.simulation_start_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as simulation_start_time,
          to_char(t.simulation_end_time, 'YYYY-DDD"T"HH24:MI:SS.FF6') as simulation_end_time
      from simulation_template as t
      where t.id = ?
    """;

  private final PreparedStatement statement;

  public GetSimulationTemplateAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<SimulationTemplateRecord> get(final long simulationTemplateId)
  throws SQLException {
      this.statement.setLong(1, simulationTemplateId);
      final ResultSet results = this.statement.executeQuery();

      if (!results.next()) return Optional.empty();

      final var modelId = results.getLong("model_id");
      final var revision = results.getLong("revision");
      final var description = results.getString("description");
      final var arguments = getJsonColumn(results, "arguments", simulationArgumentsP)
          .getSuccessOrThrow(
              failureReason -> new Error("Corrupt simulation template arguments cannot be parsed: "
                                         + failureReason.reason())
          );
      final String simStartString = results.getString("simulation_start_time");
      final String simEndString = results.getString("simulation_end_time");
      final var simStartTime = simStartString == null ? null : Timestamp.fromString(simStartString);
      final var simEndTime = simEndString == null ? null : Timestamp.fromString(simEndString);
      return Optional.of(new SimulationTemplateRecord(simulationTemplateId, revision, modelId, description, arguments, simStartTime, simEndTime));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
