package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.driver.ActivityInstanceId;
import gov.nasa.jpl.aerie.merlin.driver.SimulatedActivity;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/*package-local*/ final class UpdateSimulatedActivityParentsAction implements AutoCloseable {
  private final @Language("SQL") String sql = """
      update span
      set parent_id = ?
      where dataset_id = ?
        and id = ?
    """;

  private final PreparedStatement statement;

  public UpdateSimulatedActivityParentsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(
      final long datasetId,
      final Map<ActivityInstanceId, SimulatedActivity> simulatedActivities,
      final Map<ActivityInstanceId, Long> simIdToPgId
  ) throws SQLException {
    for (final var id : simulatedActivities.keySet()) {
      final var activity =  simulatedActivities.get(id);
      if (activity.parentId == null) continue;

      this.statement.setLong(1, simIdToPgId.get(activity.parentId));
      this.statement.setLong(2, datasetId);
      this.statement.setLong(3, simIdToPgId.get(id));

      this.statement.addBatch();
    }
    try {
      final var results = this.statement.executeBatch();
      for (final var result : results) {
        if (result != 1) throw new FailedUpdateException("span");
      }
    } finally {
      this.statement.clearBatch();
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
