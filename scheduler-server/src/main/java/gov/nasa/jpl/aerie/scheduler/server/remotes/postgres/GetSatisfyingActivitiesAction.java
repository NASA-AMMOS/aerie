package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*package-local*/ final class GetSatisfyingActivitiesAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      s.goal_id,
      s.activity_id
    from scheduling_goal_analysis_satisfying_activities as s
    where s.analysis_id = ?
    """;

  private final PreparedStatement statement;

  public GetSatisfyingActivitiesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<GoalId, List<ActivityInstanceId>> get(final long analysisId) throws SQLException {
    this.statement.setLong(1, analysisId);
    final var resultSet = this.statement.executeQuery();

    final var satisfyingActivities = new HashMap<GoalId, List<ActivityInstanceId>>();
    while (resultSet.next()) {
      final var goalId = new GoalId(resultSet.getLong("goal_id"));
      final var activityId = new ActivityInstanceId(resultSet.getLong("activity_id"));

      satisfyingActivities
          .computeIfAbsent(goalId, x -> new ArrayList<>())
          .add(activityId);
    }

    return satisfyingActivities;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
