package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.SQLException;

/*package-local*/ final class CreateSpanPartitionAction implements AutoCloseable {
  private final Connection connection;

  public CreateSpanPartitionAction(final Connection connection) {
    this.connection = connection;
  }

  public void apply(final long datasetId) throws SQLException {
    final var generatedSql = generateSql(datasetId);

    try (final var statement = connection.createStatement()) {
      statement.executeUpdate(generatedSql);
    }
  }

  private static String generateSql(final long datasetId) {
    final @Language("SQL") String sql =
        """
        create table span_%d (
           like span including defaults including constraints
        );
        alter table span
          attach partition span_%d for values in (%d);
        alter table span_%d add constraint span_has_parent_span
          foreign key (dataset_id, parent_id)
          references span_%d
          on update cascade
          on delete cascade;
        """.formatted(datasetId, datasetId, datasetId, datasetId, datasetId, datasetId, datasetId);
    return sql;
  }

  @Override
  public void close() throws SQLException {
    // Nothing to clean up. The method is an intentionally-blank override.
  }
}
