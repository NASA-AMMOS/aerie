package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import gov.nasa.jpl.aerie.types.ActivityDirectiveId;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;

/*package-local*/ final class InsertSatisfyingActivitiesAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into scheduler.scheduling_goal_analysis_satisfying_activities (analysis_id, goal_id, goal_revision, activity_id)
    values (?, ?, ?, ?)
    """;

  private final PreparedStatement statement;

  public InsertSatisfyingActivitiesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(
      final long analysisId,
      final Map<GoalId, Collection<ActivityDirectiveId>> satisfyingActivities
  ) throws SQLException {
    for (final var entry : satisfyingActivities.entrySet()) {
      final var goal = entry.getKey();
      for (final var activityId : entry.getValue()) {
        this.statement.setLong(1, analysisId);
        this.statement.setLong(2, goal.id());
        this.statement.setLong(3, goal.revision());
        this.statement.setLong(4, activityId.id());
        this.statement.addBatch();
      }
    }

    final var resultSet = this.statement.executeBatch();
    for (final var result : resultSet) {
      if (result == Statement.EXECUTE_FAILED) throw new FailedInsertException("scheduler.scheduling_goal_analysis_satisfying_activities");
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
