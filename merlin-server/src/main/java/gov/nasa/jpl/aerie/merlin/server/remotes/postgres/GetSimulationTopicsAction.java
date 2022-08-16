package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Triple;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser.valueSchemaP;

/*package-local*/ final class GetSimulationTopicsAction implements AutoCloseable {
  @Language("SQL") private final String sql = """
        select
          e.topic_index,
          e.name,
          e.value_schema
        from topic as e
        where
          e.dataset_id = ?
      """;

  private final PreparedStatement statement;

  public GetSimulationTopicsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(this.sql);
  }

  public List<Triple<Integer, String, ValueSchema>> get(
      final long datasetId) throws SQLException
  {
    this.statement.setLong(1, datasetId);
    final var resultSet = this.statement.executeQuery();

    final var topics = new ArrayList<Triple<Integer, String, ValueSchema>>();
    while (resultSet.next()) {
      topics.add(
          Triple.of(
              resultSet.getInt(1),
              resultSet.getString(2),
              parseValueSchema(resultSet.getString(3))
          )
      );
    }
    return topics;
  }

  private static ValueSchema parseValueSchema(final String valueSchemaString) {
    final ValueSchema valueSchema;
    try (
        final var valueSchemaReader = Json.createReader(new StringReader(valueSchemaString));
    ) {
      valueSchema = valueSchemaP.parse(valueSchemaReader.readValue()).getSuccessOrThrow();
    }
    return valueSchema;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
