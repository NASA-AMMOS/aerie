package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.intellij.lang.annotations.Language;

/*package-local*/ final class DeleteRequestAction implements AutoCloseable {
  private final @Language("SQL") String sql =
      """
    delete from scheduling_request
    where
      spec_id = ? and
      analysis_id = ?
    """;

  private final PreparedStatement statement;

  public DeleteRequestAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void apply(final long specId, final long analysisId) throws SQLException {
    this.statement.setLong(1, specId);
    this.statement.setLong(2, analysisId);

    this.statement.execute();
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
