package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class TemplateAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    """;

  private final PreparedStatement statement;

  public TemplateAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
