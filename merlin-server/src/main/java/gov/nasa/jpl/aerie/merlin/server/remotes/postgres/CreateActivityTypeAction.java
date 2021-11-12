package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/*package-local*/ final class CreateActivityTypeAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into activity_type (model_id, name, parameters, required_parameters)
    values (?, ?, ?::json, ?::json)
    on conflict (model_id, name) do update set parameters = ?::json, required_parameters = ?::json
    returning model_id
    """;

  private final PreparedStatement statement;

  public CreateActivityTypeAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(final long modelId, final String name, final List<Parameter> parameters, final List<String> requiredParameters)
  throws SQLException, FailedInsertException
  {
    final var order = new Object() { int value = 0; };
    final var paramMap = parameters.stream().collect(Collectors.toMap(
        p -> p.name(),
        p -> Pair.of(order.value++, p)));

    this.statement.setLong(1, modelId);
    this.statement.setString(2, name);
    PreparedStatements.setValueSchemaOrderedMap(this.statement, 3, paramMap);
    PreparedStatements.setRequiredParameters(this.statement, 4, requiredParameters);
    PreparedStatements.setValueSchemaOrderedMap(this.statement, 5, paramMap);
    PreparedStatements.setRequiredParameters(this.statement, 6, requiredParameters);

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
