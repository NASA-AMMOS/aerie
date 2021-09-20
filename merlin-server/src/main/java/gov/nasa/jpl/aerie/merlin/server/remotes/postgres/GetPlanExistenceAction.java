package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class GetPlanExistenceAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    select 1 from plan where id = ?
    """;

  private final PreparedStatement statement;

  public GetPlanExistenceAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public boolean get(final long planId) throws SQLException {
    this.statement.setLong(1, planId);

    try (final var results = this.statement.executeQuery()) {
      return results.next();
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
