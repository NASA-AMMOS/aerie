package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*package-local*/ final class UpdatePlanAction
    implements AutoCloseable  /* This isn't necessary, but it's convenient for symmetry with other actions. */
{
  private final Connection connection;

  public UpdatePlanAction(final Connection connection) {
    this.connection = connection;
  }

  public void apply(
      final long planId,
      final @Nullable String name,
      final @Nullable Timestamp startTime,
      final @Nullable Timestamp endTime
  ) throws SQLException, FailedUpdateException {
    final var generatedSql = generateSql(planId, name, startTime, endTime);

    try (final var statement = this.connection.prepareStatement(generatedSql.sql())) {
      for (var i = 1; i < generatedSql.argumentSetters().size(); i += 1) {
        generatedSql.argumentSetters().get(i).set(statement, i);
      }

      final var count = statement.executeUpdate();
      if (count != 1) throw new FailedUpdateException("plan");
    }
  }


  private interface ArgumentSetter {
    void set(PreparedStatement statement, int index) throws SQLException;
  }

  public record GeneratedSql(String sql, List<ArgumentSetter> argumentSetters) {}

  public static GeneratedSql generateSql(
      final long planId,
      final @Nullable String name,
      final @Nullable Timestamp startTime,
      final @Nullable Timestamp endTime
  ) {
    final var argumentSqlFragments = new ArrayList<String>(4);
    final var argumentSetters = new ArrayList<ArgumentSetter>(5);
    {
      if (name != null) {
        argumentSqlFragments.add("name = ?");
        argumentSetters.add((statement, index) -> statement.setString(index, name));
      }

      if (startTime != null) {
        if (endTime != null) {
          argumentSqlFragments.add("start_time = ?, duration = ? - ?");
          argumentSetters.add((statement, index) -> PreparedStatements.setTimestamp(statement, index, startTime));
          argumentSetters.add((statement, index) -> PreparedStatements.setTimestamp(statement, index, endTime));
          argumentSetters.add((statement, index) -> PreparedStatements.setTimestamp(statement, index, startTime));
        } else {
          argumentSqlFragments.add("start_time = ?, duration = end_time - ?::timestamptz");
          argumentSetters.add((statement, index) -> PreparedStatements.setTimestamp(statement, index, startTime));
          argumentSetters.add((statement, index) -> PreparedStatements.setTimestamp(statement, index, startTime));
        }
      } else {
        if (endTime != null) {
          argumentSqlFragments.add("duration = ?::timestamptz - start_time");
          argumentSetters.add((statement, index) -> PreparedStatements.setTimestamp(statement, index, endTime));
        } else {
          // pass
        }
      }
    }

    final @Language("SQL") String sql =
        "update plan set " + String.join(", ", argumentSqlFragments) + " where id = ?";

    return new GeneratedSql(sql, argumentSetters);
  }

  @Override
  public void close() {}
}
