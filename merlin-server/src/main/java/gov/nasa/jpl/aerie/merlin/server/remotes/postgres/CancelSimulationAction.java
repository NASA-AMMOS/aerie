package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package local*/ final class CancelSimulationAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    update dataset
        set canceled = true
        where id = ?
    """;

  private final PreparedStatement statement;

  public CancelSimulationAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long datasetId) throws SQLException, NoSuchDatasetException {
    this.statement.setLong(1, datasetId);
    final var count = this.statement.executeUpdate();
    if (count != 1) throw new NoSuchDatasetException(datasetId);
  }

  @Override
  public void close() throws SQLException {
    statement.close();
  }
}
