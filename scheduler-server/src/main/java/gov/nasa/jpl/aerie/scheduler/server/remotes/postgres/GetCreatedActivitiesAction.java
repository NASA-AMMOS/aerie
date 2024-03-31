package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.server.models.GoalId;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*package-local*/ final class GetCreatedActivitiesAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      c.goal_id,
      c.goal_revision,
      c.activity_id
    from scheduling_goal_analysis_created_activities as c
    where c.analysis_id = ?
    """;

  private final PreparedStatement statement;

  public GetCreatedActivitiesAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Map<GoalId, List<ActivityDirectiveId>> get(final long analysisId) throws SQLException {
    this.statement.setLong(1, analysisId);
    final var resultSet = this.statement.executeQuery();

    final var createdActivities = new HashMap<GoalId, List<ActivityDirectiveId>>();
    while (resultSet.next()) {
      final var goalId = new GoalId(resultSet.getLong("goal_id"), resultSet.getLong("goal_revision"));
      final var activityId = new ActivityDirectiveId(resultSet.getLong("activity_id"));

      if (!createdActivities.containsKey(goalId)) createdActivities.put(goalId, new ArrayList<>());
      createdActivities.get(goalId).add(activityId);
    }

    return createdActivities;
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
