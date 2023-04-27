package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.DatasetId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.intellij.lang.annotations.Language;

/*package-local*/ final class CheckPlanDatasetExistsAction implements AutoCloseable {
  private final @Language("SQL") String sql =
      """
      select 1
      from plan_dataset as p
      where
        p.dataset_id = ?
    """;

  private final PreparedStatement statement;

  public CheckPlanDatasetExistsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public boolean get(final DatasetId datasetId) throws SQLException {
    this.statement.setLong(1, datasetId.id());
    final var resultSet = statement.executeQuery();
    return resultSet.next();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
