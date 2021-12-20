package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static gov.nasa.jpl.aerie.merlin.server.http.ValueSchemaJsonParser.valueSchemaP;

/*package-local*/ final class CreateActivityTypeAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into activity_type (model_id, name, parameters, required_parameters, return_value_schema)
    values (?, ?, ?::json, ?::json, ?::json)
    on conflict (model_id, name) do update
      set parameters = ?::json,
      required_parameters = ?::json,
      return_value_schema = ?::json
    returning model_id
    """;

  private final PreparedStatement statement;

  public CreateActivityTypeAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(
      final long modelId,
      final String name,
      final List<Parameter> parameters,
      final List<String> requiredParameters,
      final Optional<ValueSchema> returnTypeValueSchema)
  throws SQLException, FailedInsertException
  {
    final var valueSchemaString = returnTypeValueSchema
        .map(s -> valueSchemaP.unparse(s).toString())
        .orElse(null);

    this.statement.setLong(1, modelId);
    this.statement.setString(2, name);
    PreparedStatements.setParameters(this.statement, 3, parameters);
    PreparedStatements.setRequiredParameters(this.statement, 4, requiredParameters);
    this.statement.setString(5, valueSchemaString);
    PreparedStatements.setParameters(this.statement, 6, parameters);
    PreparedStatements.setRequiredParameters(this.statement, 7, requiredParameters);
    this.statement.setString(8, valueSchemaString);

    try (final var results = statement.executeQuery()) {
      if (!results.next()) throw new FailedInsertException("activity_type");

      return results.getLong(1);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
