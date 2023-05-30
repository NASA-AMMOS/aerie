package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class CreateConstraintRunAction implements AutoCloseable {
  private static final @Language("SQL") String sql = """
  insert into constraint_run
    (
      constraint_id
    )
  values(?)
  returning
    status,
    id
  """;

  private final PreparedStatement preparedStatement;

  public CreateConstraintRunAction(final Connection connection) throws SQLException {
    this.preparedStatement = connection.prepareStatement(sql);
  }

  public ConstraintRunRecord apply(
      final long constraintId
  ) throws SQLException {
    this.preparedStatement.setLong(1, constraintId);

    try (final var results = this.preparedStatement.executeQuery()) {
      return new ConstraintRunRecord(
          constraintId,
          ConstraintRunRecord.Status.SUCCESS
      );
    }
  }

  @Override
  public void close() throws SQLException {
    this.preparedStatement.close();
  }
}
