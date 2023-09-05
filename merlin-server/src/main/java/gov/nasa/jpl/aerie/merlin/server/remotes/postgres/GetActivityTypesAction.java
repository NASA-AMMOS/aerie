package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.model.InputType.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.ActivityType;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static gov.nasa.jpl.aerie.json.BasicParsers.listP;
import static gov.nasa.jpl.aerie.json.BasicParsers.mapP;
import static gov.nasa.jpl.aerie.json.BasicParsers.stringP;
import static gov.nasa.jpl.aerie.merlin.driver.json.ValueSchemaJsonParser.valueSchemaP;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.getJsonColumn;
import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PostgresParsers.parameterRecordP;

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
        activityTypes.add(new ActivityType(
            results.getString("name"),
            getJsonColumn(results, "parameters", parameterRecordP)
                .getSuccessOrThrow(failureReason -> new Error("Corrupt activity type parameters cannot be parsed: "
                                                              + failureReason.reason()))
                .entrySet()
                .stream()
                .map(entry -> new ParameterRecord(
                    entry.getKey(),
                    entry.getValue().getKey(),
                    entry.getValue().getValue()))
                .sorted(Comparator.comparingInt(ParameterRecord::order))
                .map(parameterRecord -> new Parameter(parameterRecord.name(), parameterRecord.schema()))
                .toList(),
            getJsonColumn(results, "required_parameters", listP(stringP))
                .getSuccessOrThrow($ -> new Error("Corrupt activity type required parameters cannot be parsed: "
                                                  + $.reason())),
            getJsonColumn(results, "computed_attributes_value_schema", valueSchemaP)
                .getSuccessOrThrow($ -> new Error("Corrupt activity type computed attribute schema cannot be parsed: "
                                                  + $.reason())),
            getJsonColumn(results, "parameter_units", mapP(stringP))
                .getSuccessOrThrow($ -> new Error("Corrupt activity type parameter units cannot be parsed: "
                                                  + $.reason())),
            getJsonColumn(results, "computed_attribute_units", mapP(stringP))
                .getSuccessOrThrow($ -> new Error("Corrupt activity type computed attribute units cannot be parsed: "
                                                  + $.reason()))
        ));
      }
    }

    return activityTypes;
  }

  private record ParameterRecord(String name, int order, ValueSchema schema) {}

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
