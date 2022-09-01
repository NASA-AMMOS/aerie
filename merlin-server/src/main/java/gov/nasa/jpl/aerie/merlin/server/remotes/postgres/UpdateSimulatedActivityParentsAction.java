package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

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
      final Map<Long, SpanRecord> simulatedActivities,
      final Map<Long, Long> simIdToPgId
  ) throws SQLException {
    for (final var entry : simulatedActivities.entrySet()) {
      final var activity =  entry.getValue();
      final var id =  entry.getKey();
      if (activity.parentId().isEmpty()) continue;

      this.statement.setLong(1, simIdToPgId.get(activity.parentId().get()));
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
