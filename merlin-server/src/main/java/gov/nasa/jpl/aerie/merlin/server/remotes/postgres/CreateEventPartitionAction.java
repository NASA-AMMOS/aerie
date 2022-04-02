package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.SQLException;

/*package-local*/ final class CreateEventPartitionAction implements AutoCloseable {
  private final Connection connection;

  public CreateEventPartitionAction(final Connection connection) throws SQLException {
    this.connection = connection;
  }

  public void apply(final long datasetId) throws SQLException {
    final var generatedSql = generateSql(datasetId);

    try (final var statement = connection.prepareStatement(generatedSql)) {
      statement.executeUpdate();
    }
  }

  private static String generateSql(final long datasetId) {
    final @Language("SQL") String sql =
        "create table event_" + datasetId + " partition of event for values in (" + datasetId + ")";
    return sql;
  }

  @Override
  public void close() throws SQLException {
  }
}
