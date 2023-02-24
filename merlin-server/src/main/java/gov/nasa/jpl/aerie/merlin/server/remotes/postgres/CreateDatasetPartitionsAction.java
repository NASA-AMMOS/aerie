package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class CreateDatasetPartitionsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select from allocate_dataset_partitions(?);
  """;

  private final PreparedStatement statement;

  public CreateDatasetPartitionsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long datasetId) throws SQLException {
    this.statement.setLong(1, datasetId);
    this.statement.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
