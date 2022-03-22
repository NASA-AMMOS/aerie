package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.simulationArgumentsP;

/*package local*/ final class GetSimulationAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
          s.id,
          s.revision,
          s.simulation_template_id,
          s.arguments
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
      final ResultSet results = this.statement.executeQuery();

      if (!results.next()) return Optional.empty();

      final var id = results.getLong(1);
      final var revision = results.getLong(2);
      final var templateId$ =
          results.getObject(3) == null ?
              Optional.<Long>empty() :
              Optional.of(results.getLong(3));
      final var arguments = parseSimulationArguments(results.getCharacterStream(4));
      return Optional.of(new SimulationRecord(id, revision, planId, templateId$, arguments));
  }

  private Map<String, SerializedValue> parseSimulationArguments(final Reader stream) {
    final var json = Json.createReader(stream).readValue();
    return simulationArgumentsP
        .parse(json)
        .getSuccessOrThrow(
            failureReason -> new Error("Corrupt simulation arguments cannot be parsed: " + failureReason.reason())
        );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
