package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class DeleteDatasetAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    delete from dataset
    where id = ?
    """;

  private final PreparedStatement statement;

  public DeleteDatasetAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public boolean apply(final long datasetId) throws SQLException {
    this.statement.setLong(1, datasetId);
    return this.statement.execute();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
