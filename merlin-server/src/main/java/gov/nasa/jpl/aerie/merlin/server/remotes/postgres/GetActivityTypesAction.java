package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import org.intellij.lang.annotations.Language;

import javax.json.Json;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.intP;
import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.productP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.merlin.server.http.ValueSchemaJsonParser.valueSchemaP;

/*package-local*/ final class GetActivityTypesAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      a.name,
      a.parameters,
      a.required_parameters,
      a.computed_attributes_value_schema
    from activity_type as a
    where a.model_id = ?
    """;

  private final PreparedStatement statement;

  public GetActivityTypesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<ActivityType> get(final long missionModelId) throws SQLException {
    this.statement.setLong(1, missionModelId);

    final var activityTypes = new ArrayList<ActivityType>();
    try (final var results = this.statement.executeQuery()) {
      while (results.next()) {
        activityTypes.add(
            new ActivityType(
                results.getString("name"),
                parseParameters(results.getCharacterStream("parameters")),
                parseRequiredParameters(results.getCharacterStream("required_parameters")),
                parseComputedAttributesValueSchema(results.getCharacterStream("computed_attributes_value_schema"))));
      }
    }

    return activityTypes;
  }

  private record ParameterRecord(String name, int order, ValueSchema schema) {}
  private static List<Parameter> parseParameters(final Reader stream) {
    final var json = Json.createReader(stream).readValue();
    final var parametersMap =
        mapP(productP
                 .field("order", intP)
                 .field("schema", valueSchemaP))
        .parse(json)
        .getSuccessOrThrow(
            failureReason -> new Error("Corrupt activity type parameters cannot be parsed: " + failureReason.reason())
        );
    return parametersMap
        .entrySet()
        .stream()
        .map(entry -> new ParameterRecord(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue()))
        .sorted(Comparator.comparingInt(ParameterRecord::order))
        .map(record -> new Parameter(record.name(), record.schema()))
        .toList();
  }

  private static List<String> parseRequiredParameters(final Reader stream) {
    final var json = Json.createReader(stream).readValue();
    return listP(stringP)
        .parse(json)
        .getSuccessOrThrow(
            failureReason -> new Error("Corrupt activity type required parameters cannot be parsed: " + failureReason.reason())
        );
  }

  private static ValueSchema parseComputedAttributesValueSchema(final Reader stream) {
    final var json = Json.createReader(stream).readValue();
    return valueSchemaP
        .parse(json)
        .getSuccessOrThrow(
            failureReason -> new Error("Corrupt activity type computed attribute schema cannot be parsed: " + failureReason.reason())
        );
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
