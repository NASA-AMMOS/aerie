package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class CreateUploadedFileAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into uploaded_file (path, name)
    values (?, ?)
    returning id
    """;

  private final PreparedStatement statement;

  public CreateUploadedFileAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(final String name, final Path path) throws SQLException {
    this.statement.setString(1, path.toString());
    this.statement.setString(2, name);

    try (final var results = statement.executeQuery()) {
      if (!results.next()) throw new FailedInsertException("mission_model");

      return results.getLong(1);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
