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

/*package local*/ final class GetSimulationTemplateAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
          t.model_id,
          t.revision,
          t.description,
          t.arguments
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
      final var arguments = parseSimulationArguments(results.getCharacterStream(4));
      return Optional.of(new SimulationTemplateRecord(simulationTemplateId, revision, modelId, description, arguments));
  }

  private Map<String, SerializedValue> parseSimulationArguments(final Reader stream) {
    try(final var reader = Json.createReader(stream)) {
      final var json = reader.readValue();
      return simulationArgumentsP
          .parse(json)
          .getSuccessOrThrow(
              failureReason -> new Error("Corrupt simulation template arguments cannot be parsed: "
                                         + failureReason.reason())
          );
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}

