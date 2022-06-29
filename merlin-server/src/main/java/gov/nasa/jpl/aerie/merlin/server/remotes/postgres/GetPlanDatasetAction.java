package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*package-local*/ final class GetPlanDatasetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select
      dataset_id
    from plan_dataset
    where plan_id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public List<Long> get(final long planId) throws SQLException, NoSuchPlanException {
    this.statement.setLong(1, planId);

    try (final var results = this.statement.executeQuery()) {
      final var datasetIds = new ArrayList<Long>();
      while(results.next()) {
        final var datasetId = results.getLong(1);
        datasetIds.add(datasetId);
      }

      return datasetIds;
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
