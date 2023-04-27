package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.intellij.lang.annotations.Language;

/*package-local*/ final class GetSpecificationConditionsAction implements AutoCloseable {
  private final @Language("SQL") String sql =
      """
    select
      s.condition_id,
      s.enabled,
      c.name,
      c.definition,
      c.revision
    from scheduling_specification_conditions as s
      join scheduling_condition as c
      on s.specification_id = ?
      and s.condition_id = c.id
    """;

  private final PreparedStatement statement;

  public GetSpecificationConditionsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<PostgresSchedulingConditionRecord> get(final long specificationId)
      throws SQLException {
    this.statement.setLong(1, specificationId);
    final var resultSet = this.statement.executeQuery();

    final var goals = new ArrayList<PostgresSchedulingConditionRecord>();
    while (resultSet.next()) {
      final var id = resultSet.getLong("condition_id");
      final var revision = resultSet.getLong("revision");
      final var name = resultSet.getString("name");
      final var definition = resultSet.getString("definition");
      final var enabled = resultSet.getBoolean("enabled");
      goals.add(new PostgresSchedulingConditionRecord(id, revision, name, definition, enabled));
    }

    return goals;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
