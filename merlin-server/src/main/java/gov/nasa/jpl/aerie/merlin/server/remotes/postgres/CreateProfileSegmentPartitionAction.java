package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.SQLException;

/*package-local*/ final class CreateProfileSegmentPartitionAction implements AutoCloseable {
  private final Connection connection;

  public CreateProfileSegmentPartitionAction(final Connection connection) throws SQLException {
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
        "create table profile_segment_" + datasetId + " partition of profile_segment for values in (" + datasetId + ")";
    return sql;
  }

  @Override
  public void close() throws SQLException {
    // Nothing to clean up. The method is an intentionally-blank override.
  }
}
