package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setSerializedValue;

/*package-local*/ final class CreateActivityArgumentsAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into activity_argument (activity_id, name, value)
    values (?, ?, ?)
    """;

  private final PreparedStatement statement;

  public CreateActivityArgumentsAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public void add(
      final long activityId,
      final String name,
      final SerializedValue value
  ) throws SQLException {
    this.statement.setLong(1, activityId);
    this.statement.setString(2, name);
    setSerializedValue(this.statement, 3, value);

    this.statement.addBatch();
  }

  public void apply() throws SQLException, FailedInsertException {
    try {
      final var results = this.statement.executeBatch();
      for (final var result : results) {
        if (result != 1) throw new FailedInsertException("activity_argument");
      }
    } finally {
      this.statement.clearBatch();
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
