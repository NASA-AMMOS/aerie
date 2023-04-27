package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.intellij.lang.annotations.Language;

/*package-local*/ final class GetSpecificationGoalsAction implements AutoCloseable {
  private final @Language("SQL") String sql =
      """
    select
      s.goal_id,
      g.name,
      g.definition,
      g.revision,
      s.enabled,
      s.simulate_after
    from scheduling_specification_goals as s
    left join scheduling_goal as g on s.goal_id = g.id
    where s.specification_id = ?
    order by s.priority;
    """;

  private final PreparedStatement statement;

  public GetSpecificationGoalsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<PostgresGoalRecord> get(final long specificationId) throws SQLException {
    this.statement.setLong(1, specificationId);
    final var resultSet = this.statement.executeQuery();

    final var goals = new ArrayList<PostgresGoalRecord>();
    while (resultSet.next()) {
      final var id = resultSet.getLong("goal_id");
      final var revision = resultSet.getLong("revision");
      final var name = resultSet.getString("name");
      final var definition = resultSet.getString("definition");
      final var enabled = resultSet.getBoolean("enabled");
      final var simulateAfter = resultSet.getBoolean("simulate_after");
      goals.add(new PostgresGoalRecord(id, revision, name, definition, enabled, simulateAfter));
    }

    return goals;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
