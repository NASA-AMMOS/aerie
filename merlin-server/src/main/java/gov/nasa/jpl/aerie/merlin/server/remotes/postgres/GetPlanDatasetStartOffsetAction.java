package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class GetPlanDatasetStartOffsetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      offset_from_plan_start::timestamptz
    from plan_dataset
    where plan_id = ?
    and dataset_id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanDatasetStartOffsetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public Timestamp get(final long planId, final long datasetId) throws SQLException, NoSuchPlanException {
    this.statement.setLong(1, planId);
    this.statement.setLong(2, datasetId);

    try (final var results = this.statement.executeQuery()) {
      final var ts = results.getTimestamp(1);
      return Timestamp.fromString(ts.toString());
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
