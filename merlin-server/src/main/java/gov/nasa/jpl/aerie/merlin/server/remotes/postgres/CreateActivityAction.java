package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static gov.nasa.jpl.aerie.merlin.server.remotes.postgres.PreparedStatements.setTimestamp;

/*package-local*/ final class CreateActivityAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
    insert into activity (plan_id, start_offset, type)
    values (?, ?::timestamptz - ?::timestamptz, ?)
    returning id
    """;

  private final PreparedStatement statement;

  public CreateActivityAction(final Connection connection) throws SQLException {
    this.statement = connection.prepareStatement(sql);
  }

  public long apply(
      final long planId,
      final Timestamp planStartTime,
      final Timestamp activityStartTime,
      final String type
  ) throws SQLException, FailedInsertException {
    this.statement.setLong(1, planId);
    setTimestamp(this.statement, 2, activityStartTime);
    setTimestamp(this.statement, 3, planStartTime);
    this.statement.setString(4, type);

    try (final var results = this.statement.executeQuery()) {
      if (!results.next()) throw new FailedInsertException("activity");

      return results.getLong(1);
    }
  }

  @Override
  public void close() throws SQLException {
    this.statement.close();
  }
}
