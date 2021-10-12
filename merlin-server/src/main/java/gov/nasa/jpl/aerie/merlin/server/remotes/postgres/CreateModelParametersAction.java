package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.server.http.ResponseSerializers;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/*package-local*/ final class CreateModelParametersAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into mission_model_parameters (model_id, parameters)
    values (?, ?::json)
    on conflict (model_id) do update set parameters = ?::json
    returning model_id
    """;

  private final PreparedStatement statement;

  public CreateModelParametersAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(final long modelId, final List<Parameter> parameters) throws SQLException, FailedInsertException {
    this.statement.setLong(1, modelId);
    final var paramMap = parameters.stream().collect(Collectors.toMap(Parameter::name, Parameter::schema));
    final var paramJson = ResponseSerializers.serializeMap(ResponseSerializers::serializeValueSchema, paramMap).toString();
    this.statement.setString(2, paramJson);
    this.statement.setString(3, paramJson);

    try (final var results = statement.executeQuery()) {
      if (!results.next()) throw new FailedInsertException("mission_model_parameters");

      return results.getLong(1);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
