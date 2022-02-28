package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*package-local*/ final class GetSpecificationGoalsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
    with
      goals as
        ( select
            s.specification_id,
            s.goal_id,
            s.priority,
            g.definition,
            g.revision
          from scheduling_specification_goals as s
            left join scheduling_goal as g
            on s.goal_id = g.id )
    select
      g.goal_id,
      g.definition,
      g.revision
    from goals as g
      where g.specification_id = ?
      order by g.priority asc
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
      final var definition = resultSet.getString("definition");
      goals.add(new PostgresGoalRecord(id, revision, definition));
    }

    return goals;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
