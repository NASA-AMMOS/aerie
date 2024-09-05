package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.merlin.driver.json.SerializedValueJsonParser.serializedValueP;

/*package-local*/ final class GetSimulationFinconsAction implements AutoCloseable {
  @Language("SQL") private final String sql = """
        select
          e.fincons
        from merlin.simulation_fincons as e
        where
          e.dataset_id = ?
      """;

  private final PreparedStatement statement;

  public GetSimulationFinconsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(this.sql);
  }

  public SerializedValue get(final long datasetId) throws SQLException
  {
    this.statement.setLong(1, datasetId);
    final var resultSet = this.statement.executeQuery();
    resultSet.next();
    return parseSerializedValue(resultSet.getString(1));

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
