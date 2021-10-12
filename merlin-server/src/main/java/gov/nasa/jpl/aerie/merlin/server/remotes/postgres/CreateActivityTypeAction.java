package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/*package-local*/ final class CreateActivityTypeAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into activity_type (model_id, name, parameters)
    values (?, ?, ?::json)
    on conflict (model_id, name) do update set parameters = ?::json
    returning model_id
    """;

  private final PreparedStatement statement;

  public CreateActivityTypeAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(final long modelId, final String name, final List<Parameter> parameters) throws SQLException, FailedInsertException {
    this.statement.setLong(1, modelId);
    final var paramMap = parameters.stream().collect(Collectors.toMap(Parameter::name, Parameter::schema));
    this.statement.setString(2, name);
    PreparedStatements.setValueSchemaMap(this.statement, 3, paramMap);
    PreparedStatements.setValueSchemaMap(this.statement, 4, paramMap);

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
