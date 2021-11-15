package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/*package-local*/ final class CreateModelAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into mission_model (name, version, mission, owner, jar_id)
    values (?, ?, ?, ?, ?)
    returning id
    """;

  private final PreparedStatement statement;

  public CreateModelAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(
      final String name,
      final String version,
      final String mission,
      final String owner,
      final long jarId
  ) throws SQLException {
    this.statement.setString(1, name);
    this.statement.setString(2, version);
    this.statement.setString(3, mission);
    this.statement.setString(4, owner);
    this.statement.setLong(5, jarId);

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
