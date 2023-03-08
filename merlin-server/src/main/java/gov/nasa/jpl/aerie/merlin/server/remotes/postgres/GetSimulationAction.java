package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.getJsonColumn;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.simulationArgumentsP;

/*package local*/ final class GetSimulationAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
          s.id,
          s.revision,
          s.simulation_template_id,
          s.arguments,
          s.offset_from_plan_start,
          s.duration
      from simulation as s
      where s.plan_id = ?
    """;

  private final PreparedStatement statement;

  public GetSimulationAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Optional<SimulationRecord> get(final long planId)
  throws SQLException {
      this.statement.setLong(1, planId);
      PreparedStatements.setIntervalStyle(this.statement.getConnection(), PreparedStatements.PGIntervalStyle.ISO8601);

      final ResultSet results = this.statement.executeQuery();

      if (!results.next()) return Optional.empty();

      final var id = results.getLong(1);
      final var revision = results.getLong(2);
      final var templateId$ =
          results.getObject(3) == null ?
              Optional.<Long>empty() :
              Optional.of(results.getLong(3));
      final var arguments = getJsonColumn(results, "arguments", simulationArgumentsP)
          .getSuccessOrThrow(
              failureReason -> new Error("Corrupt simulation arguments cannot be parsed: " + failureReason.reason())
          );
      final var offsetFromPlanStart = results.getObject(5) == null ? Optional.<Duration>empty() : Optional.of(PostgresParsers.parseDurationISO8601(results.getString(5)));
      final var duration = results.getObject(6) == null ? Optional.<Duration>empty() : Optional.of(PostgresParsers.parseDurationISO8601(results.getString(6)));
      return Optional.of(new SimulationRecord(id, revision, planId, templateId$, arguments, offsetFromPlanStart, duration));
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
