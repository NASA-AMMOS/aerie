package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
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
          t.offset_from_plan_start,
          t.duration
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

      final var modelId = results.getLong(1);
      final var revision = results.getLong(2);
      final var description = results.getString(3);
      final var arguments = getJsonColumn(results, "arguments", simulationArgumentsP)
          .getSuccessOrThrow(
              failureReason -> new Error("Corrupt simulation template arguments cannot be parsed: "
                                         + failureReason.reason())
          );
      final var offsetFromPlanStart = results.getObject(5) == null ? Optional.<Duration>empty() : Optional.of(PostgresParsers.parseDurationISO8601(results.getString(5)));
      final var duration = results.getObject(6) == null ? Optional.<Duration>empty() : Optional.of(PostgresParsers.parseDurationISO8601(results.getString(6)));
      return Optional.of(new SimulationTemplateRecord(simulationTemplateId, revision, modelId, description, arguments, offsetFromPlanStart, duration));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
